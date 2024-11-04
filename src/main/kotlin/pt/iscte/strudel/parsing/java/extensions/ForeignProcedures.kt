package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.strudel.parsing.java.*
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.PolymophicProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Reference
import pt.iscte.strudel.vm.impl.Value
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun foreign(
    module: IModule,
    method: Method,
    types: Map<String, IType>,
    location: Node
): ForeignProcedure {
    val thisParamType =
        if (Modifier.isStatic(method.modifiers)) listOf() else listOf(
            getTypeByName(method.declaringClass.canonicalName, location, types)
        )
    val parameterTypes = thisParamType + method.parameters.map {
        getTypeByName(
            it.type.canonicalName,
            location,
            types
        )
    }
    return ForeignProcedure(
        module,
        method.declaringClass.primitiveType.canonicalName,
        method.name,
        getTypeByName(method.returnType.canonicalName, location, types),
        parameterTypes
    )
    { vm, args ->
        val jvmArgs = args.map {
            it.toJvm()
        }
        val res =
            if (Modifier.isStatic(method.modifiers)) // static --> no caller
                method.invoke(null, *jvmArgs.toTypedArray())
            else if (args.size == 1) { // not static --> caller is $this parameter
                method.invoke(jvmArgs.first())
            } else if (args.size > 1) {
                val caller = jvmArgs.first()
                if (caller!!.javaClass != method.declaringClass)
                    error("Cannot invoke instance method $method with object instance $caller: is ${caller.javaClass.canonicalName}, should be ${method.declaringClass.canonicalName}")
                val arguments = jvmArgs.drop(1)
                method.invoke(caller, *arguments.toTypedArray())
            } else
                error("Cannot invoke instance method $method with 0 arguments: missing (at least) object reference $THIS_PARAM")

        // propagate side effects in arrays
        jvmArgs.forEachIndexed  { i, p ->
            if(!jvmArgs::class.java.isPrimitive && args[i] is IReference<*>) {
                val srcTarget = (args[i] as IReference<*>).target
                if(srcTarget is IArray) {
                    for(i in 0 until srcTarget.length) {
                        val e = jvmToStrudel(
                            vm,
                            types,
                            p!!::class.java.componentType,
                            Array.get(p, i)
                        )
                        if(e != srcTarget.getElement(i))
                            srcTarget.setElement(i, e)
                    }
                }
            }
        }
        jvmToStrudel(vm, types, method.returnType, res)
    }
}

internal fun IValue.toJvm(): Any? =
    if (this is IReference<*>)
        if(isNull)
            null
        else if (target is IArray) {
            val len = (target as IArray).length
            val a = Array.newInstance(
                target.type.asArrayType.componentType.toJvm,
                len
            )
            for(i in 0 until len)
                Array.set(a, i, (target as IArray).getElement(i).toJvm())
            a
        } else
            target.value
    else
        value

internal fun jvmToStrudel(
    vm: IVirtualMachine,
    types: Map<String, IType>,
    type: Class<*>,
    res: Any?
): IValue =
    if (res == null)
        vm.getNullReference()
    else if (type.isPrimitive)
        vm.getValue(res)
    else if (res is String)
        vm.allocateString(res)
    else if (type.isArray) {
        val len = Array.getLength(res)
        val ref = vm.allocateArray(types[type.componentType.canonicalName]?:ANY, len)
        for (i in 0 until len) {
            when(type.componentType) {
                Int::class.java -> ref.target.setElement(i, jvmToStrudel(vm, types, Int::class.java, Array.getInt(res, i)))
                Double::class.java -> ref.target.setElement(i, jvmToStrudel(vm, types, Int::class.java, Array.getDouble(res, i)))
                Char::class.java -> ref.target.setElement(i, jvmToStrudel(vm, types, Int::class.java,Array.getChar(res, i)))
                Boolean::class.java -> ref.target.setElement(i, jvmToStrudel(vm, types, Int::class.java, Array.getBoolean(res, i)))
                else -> ref.target.setElement(i, jvmToStrudel(vm, types, Any::class.java, Array.get(res, i)))
            }
        }
        ref
    } else
        // TODO excluded from memory
        Reference(
            Value(
                types[type.canonicalName] ?: UnboundRecordType(
                    type.canonicalName ?: ""
                ), res
            )
        )

internal fun MethodCallExpr.asForeignProcedure(
    module: IModule,
    namespace: String?,
    types: Map<String, IType>
): IProcedureDeclaration? {
    fun Class<*>.rootComponentType(): Class<*>? {
        var current = this.componentType
        while (current != null && current.componentType != null)
            current = current.componentType
        return current
    }

    if (isAbstractMethodCall) {
        if (namespace != null) {
            val clazz: Class<*> = getClassByName(namespace, this)
            (module.types + defaultTypes.values).forEach {
                kotlin.runCatching { it.toJavaType(this) }.onSuccess {
                    val t: Class<*> = (it.rootComponentType() ?: it).wrapperType
                    if (clazz.isAssignableFrom(t) && !t.isInterface) {
                        val args =
                            arguments.map { arg -> arg.getResolvedJavaType() }
                        val implementation =
                            t.findCompatibleMethod(nameAsString, args)
                        if (implementation != null)
                            module.members.add(
                                foreign(
                                    module,
                                    implementation,
                                    types,
                                    this
                                )
                            )
                    }
                }
            }
        }
        return PolymophicProcedure(
            module,
            namespace,
            nameAsString,
            this.resolve().returnType.toIType(types, this)
        )
    } else if (scope.isPresent) {
        kotlin.runCatching {
//            if (nameAsString == "equals")
//                return ForeignProcedure(
//                    module,
//                    namespace,
//                    "equals",
//                    BOOLEAN,
//                    arguments.map { it.getResolvedIType(types) }
//                ) { vm, args ->
//                    vm.getValue(args.all { it == args.first() })
//                }

            val clazz: Class<*> = scope.get().getResolvedJavaType()

            val args = arguments.map { it.getResolvedJavaType() }
            val method: Method =
                clazz.findCompatibleMethod(nameAsString, args) ?: return null

            return foreign(module, method, types, this)
        }.getOrElse { return null }
    }
    return null
}

internal fun ObjectCreationExpr.asForeignProcedure(
    module: IModule,
    types: Map<String, IType>
): ForeignProcedure {
    val parameterTypes =
        arguments.map { it.getResolvedJavaType() }.toTypedArray()
    val constructor = getResolvedJavaType().getConstructor(*parameterTypes)
    val type =
        getTypeByName(constructor.declaringClass.canonicalName, this, types)
    return ForeignProcedure(
        module,
        constructor.declaringClass.canonicalName,
        constructor.name,
        type,
        arguments.map { it.getResolvedIType(types) }
    )
    { _, a ->
        val args = a.map {
            if (it.type.isArrayReference) {
                val strudelArray = (it as IReference<*>).target as IArray
                val len = strudelArray.length
                val array = Array.newInstance(
                    ((it.type as IReferenceType).target as IArrayType).componentType.toJvm,
                    len
                )
                for (i in 0 until len)
                    Array.set(array, i, strudelArray.getElement(i).value)
                array
            } else if (it.type.isRecordReference)
                (it as IReference<*>).target.value
            else
                it.value
        }
        val obj = constructor.newInstance(*args.toTypedArray())
        Reference(Value(type, obj))
    }
}

internal val IType.toJvm
    get() =
        when (this) {
            INT -> Int::class.java
            DOUBLE -> Double::class.java
            CHAR -> Char::class.java
            BOOLEAN -> BOOLEAN::class.java
            else -> Any::class.java
        }


internal fun ResolvedType.foreignStaticFieldAccess(
    module: IModule,
    types: Map<String, IType>,
    location: Node
): ForeignProcedure {
    val java = toJavaType(location)
    return ForeignProcedure(
        module,
        java.canonicalName,
        "getField",
        getTypeByName(java.canonicalName, location, types),
        listOf(StringType)
    )
    { _, args ->
        Value(
            HostRecordType(java.canonicalName),
            java.getField(args.first().value as String).get(null)
        )
    }
}
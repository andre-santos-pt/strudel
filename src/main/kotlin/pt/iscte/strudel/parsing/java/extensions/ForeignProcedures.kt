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
        val a = args.map {
            if (it is IReference<*>)
                it.target.value
            else
                it.value
        }
        val res =
            if (Modifier.isStatic(method.modifiers)) // static --> no caller
//                method.invoke(null, *args.map { it.value }.toTypedArray())
                method.invoke(null, *a.toTypedArray())
            else if (args.size == 1) { // not static --> caller is $this parameter
                method.invoke(a.first())
            } else if (args.size > 1) {
//                val caller = when (val first = args.first().value) {
//                    is IValue -> first.value
//                    else -> first
//                }
                val caller = a.first()
                if (caller!!.javaClass != method.declaringClass)
                    error("Cannot invoke instance method $method with object instance $caller: is ${caller.javaClass.canonicalName}, should be ${method.declaringClass.canonicalName}")
                //val arguments = args.slice(1 until args.size).map { it.value }
                val arguments = a.drop(1)
                method.invoke(caller, *arguments.toTypedArray())
            } else
                error("Cannot invoke instance method $method with 0 arguments: missing (at least) object reference $THIS_PARAM")
//        when (res) {
//            is String -> getString(res)
//            else -> vm.getValue(res)
//        }
        if (method.returnType.isPrimitive)
            vm.getValue(res)
        else if (res is String)
            vm.allocateString(res)
        else
            Value(
                types[method.returnType.canonicalName] ?: UnboundRecordType(
                    method.returnType.canonicalName ?: ""
                ), res
            )
    }
}

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
            if(it.type.isArrayReference) {
                val strudelArray = (it as IReference<*>).target as IArray
                val len = strudelArray.length
                val array = Array.newInstance(
                    ((it.type as IReferenceType).target as IArrayType).componentType.toJvm,
                    len
                )
                for(i in 0 until len)
                    Array.set(array, i, strudelArray.getElement(i).value)
                array
            }
            else if(it.type.isRecordReference)
                (it as IReference<*>).target.value
            else
                it.value
        }
        val obj = constructor.newInstance(*args.toTypedArray())
        Reference(Value(type, obj))
    }
}

internal val IType.toJvm get() =
    when(this) {
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
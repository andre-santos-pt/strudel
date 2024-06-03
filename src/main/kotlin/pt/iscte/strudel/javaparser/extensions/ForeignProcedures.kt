package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.strudel.javaparser.THIS_PARAM
import pt.iscte.strudel.javaparser.StringType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.PolymophicProcedure
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Value
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun foreign(module: IModule, method: Method, types: Map<String, IType>): ForeignProcedure {
    val thisParamType = if (Modifier.isStatic(method.modifiers)) listOf() else listOf(getTypeByName(method.declaringClass.canonicalName, types))
    val parameterTypes = thisParamType + method.parameters.map { getTypeByName(it.type.canonicalName, types) }
    return ForeignProcedure(
        module,
        method.declaringClass.primitiveType.canonicalName,
        method.name,
        getTypeByName(method.returnType.canonicalName, types),
        parameterTypes
    )
    { vm, args ->
        val res =
            if (Modifier.isStatic(method.modifiers)) // static --> no caller
                method.invoke(null, *args.map { it.value }.toTypedArray())
            else if (args.size == 1) { // not static --> caller is $this parameter
                method.invoke(args.first().value)
            } else if (args.size > 1) {
                val caller = when (val first = args.first().value) {
                    is IValue -> first.value
                    else -> first
                }
                if (caller!!.javaClass != method.declaringClass)
                    error("Cannot invoke instance method $method with object instance $caller: is ${caller.javaClass.canonicalName}, should be ${method.declaringClass.canonicalName}")
                val arguments = args.slice(1 until args.size).map { it.value }
                method.invoke(caller, *arguments.toTypedArray())
            } else error("Cannot invoke instance method $method with 0 arguments: missing (at least) object reference $THIS_PARAM")
        when (res) {
            is String -> getString(res)
            else -> vm.getValue(res)
        }
    }
}

internal fun MethodCallExpr.asForeignProcedure(module: IModule, namespace: String?, types: Map<String, IType>): IProcedureDeclaration? {
    fun Class<*>.rootComponentType(): Class<*>? {
        var current = this.componentType
        while (current != null && current.componentType != null)
            current = current.componentType
        return current
    }

    if (isAbstractMethodCall) {
        //println("Handling abstract method call $this for namespace $namespace...")
        if (namespace != null) {
            val clazz: Class<*> = getClassByName(namespace)
            module.types.forEach {
                kotlin.runCatching { it.toJavaType() }.onSuccess {
                    val t: Class<*> = (it.rootComponentType() ?: it).wrapperType
                    //println("\tFound type: ${t.canonicalName}. Is ${clazz.simpleName} assignable from ${t.simpleName}? ${clazz.isAssignableFrom(t)}")
                    if (clazz.isAssignableFrom(t) && !t.isInterface) {
                        val args = arguments.map { arg -> arg.getResolvedJavaType() }
                        //println("\t${clazz.canonicalName} is assignable from ${t.canonicalName}, finding method ${t.canonicalName}.$nameAsString(${args.joinToString { it.canonicalName }})...")
                        val implementation = t.findCompatibleMethod(nameAsString, args)
                        if (implementation != null) {
                            //println("\tAdding method $implementation to module because ${clazz.canonicalName} is assignable from ${t.canonicalName}")
                            module.members.add(foreign(module, implementation, types))
                        }
                    }
                }
            }
        }
        return PolymophicProcedure(module, namespace, nameAsString, this.resolve().returnType.toIType(types))
    }
    else if (scope.isPresent) {
        kotlin.runCatching {
            val clazz: Class<*> = scope.get().getResolvedJavaType()

            val args = arguments.map { it.getResolvedJavaType() }.toTypedArray()
            val method: Method = clazz.getMethod(nameAsString, *args)

            return foreign(module, method, types)
        }.getOrElse {
            //println("Failed to find method for method call $this: $it")
            return null
        }
    }
    return null
}

internal fun ObjectCreationExpr.asForeignProcedure(module: IModule, types: Map<String, IType>): ForeignProcedure {
    val parameterTypes = arguments.map { it.getResolvedJavaType() }.toTypedArray()
    val constructor = getResolvedJavaType().getConstructor(*parameterTypes)

    return ForeignProcedure(
        module,
        constructor.declaringClass.canonicalName,
        constructor.name,
        getTypeByName(constructor.declaringClass.canonicalName, types),
        arguments.map { it.getResolvedIType(types) }
    )
    { vm, args ->
        Value(
            HostRecordType(constructor.declaringClass.canonicalName),
            constructor.newInstance(*args.slice(1 until args.size).map { it.value }.toTypedArray())
        )
    }
}

internal fun ResolvedType.foreignStaticFieldAccess(module: IModule, types: Map<String, IType>): ForeignProcedure {
    val java = toJavaType()
    return ForeignProcedure(
        module,
        java.canonicalName,
        "getField",
        getTypeByName(java.canonicalName, types),
        listOf(StringType)
    )
    { vm, args ->
        val fieldId = args.first().value as String
        Value(HostRecordType(java.canonicalName), java.getField(fieldId).get(null))
    }
}
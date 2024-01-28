package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import pt.iscte.strudel.javaparser.unsupported
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.vm.impl.ForeignProcedure
import java.lang.reflect.Method

internal fun createForeignProcedure(scope: String?, method: Method, isStatic: Boolean, types: Map<String, IType>): ForeignProcedure {
    val thisParam = if (isStatic) listOf() else listOf(getType(method.declaringClass.simpleName, types))
    val parameterTypes = thisParam + method.parameters.map { getType(it.type.typeName, types) }
    return ForeignProcedure(
        null,
        scope,
        method.name,
        getType(method.returnType.typeName, types),
        parameterTypes
    )
    { vm, args ->
        val res =
            if (isStatic) // static --> no caller
                method.invoke(null, *args.map { it.value }.toTypedArray())
            else if (args.size == 1) { // not static --> caller is $this parameter
                method.invoke(args.first().value)
            } else if (args.size > 1) {
                val caller = args.first().value // should be $this
                if (caller!!.javaClass != method.declaringClass)
                    error("Cannot invoke instance method $method with object instance $caller: is ${caller.javaClass.canonicalName}, should be ${method.declaringClass.canonicalName}")
                val arguments = args.slice(1 until args.size).map { it.value }
                method.invoke(caller, *arguments.toTypedArray())
            } else error("Cannot invoke instance method $method with 0 arguments: missing (at least) object instance")
        when (res) {
            is String -> getStringValue(res)
            else -> vm.getValue(res)
        }
    }
}

internal fun MethodCallExpr.asForeignProcedure(types: Map<String, IType>): IProcedureDeclaration? {
    if (isAbstractMethodCall) return null
    if (scope.isPresent) {
        val namespace = scope.get()
        if (namespace is NameExpr) {
            val clazz: Class<*> = runCatching {
                getClass(namespace.toString())
            }.getOrNull() ?: namespace.getResolvedJavaType()

            val args = arguments.map { it.getResolvedJavaType() }.toTypedArray()
            val method: Method = clazz.getMethod(nameAsString, *args)

            val isStaticMethod = this.resolve().isStatic
            return createForeignProcedure(namespace.toString(), method, isStaticMethod, types)
        } else
            unsupported("automatic foreign procedure creation for method call expression: $this", this)
    }
    return null
}
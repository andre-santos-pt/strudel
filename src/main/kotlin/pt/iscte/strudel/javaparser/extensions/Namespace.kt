package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.resolution.types.ResolvedReferenceType
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IType

data class Namespace(val qualifiedName: String, val isAbstract: Boolean, val isStatic: Boolean)

internal fun MethodCallExpr.getNamespace(types: Map<String, IType>, foreignProcedures: List<IProcedureDeclaration>): Namespace? =
    if (scope.isPresent) {
        val scope = scope.get()

        val scopeIsCurrentlyLoadedType: Boolean = scope is NameExpr && types.containsKey(scope.toString())
        val scopeIsCurrentlyLoadedForeignType: Boolean = foreignProcedures.any { it.namespace == scope.toString() }
        val scopeIsValidJavaClass: Boolean = isJavaClassName(scope.toString())

        if (scopeIsCurrentlyLoadedType || scopeIsCurrentlyLoadedForeignType || scopeIsValidJavaClass)
            Namespace(scope.toString(), isAbstract = false, isStatic = true)
        else if (isAbstractMethodCall) when (val type = scope.calculateResolvedType()) {
            is ResolvedReferenceType -> Namespace(type.qualifiedName, isAbstract = true, isStatic = false)
            else -> null
        } else Namespace(scope.getResolvedJavaType().canonicalName, isAbstract = false, isStatic = false)
    } else null
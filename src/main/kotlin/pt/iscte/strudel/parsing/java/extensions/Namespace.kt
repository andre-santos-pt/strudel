package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IType

data class Namespace(
    val qualifiedName: String,
    val isAbstract: Boolean,
    val isStatic: Boolean
)


fun NameExpr.resolveLocalDeclaration(): ResolvedType? {
    val m = this.findAncestor(MethodDeclaration::class.java).getOrNull ?: return null
    m.parameters.find { it.nameAsString == this.nameAsString }?.let {
        return it.type.resolve()
    }

    val decs = mutableListOf<VariableDeclarationExpr>()
    m.accept(object: VoidVisitorAdapter<MutableList<VariableDeclarationExpr>>() {
        override fun visit(n: VariableDeclarationExpr, decs: MutableList<VariableDeclarationExpr>) {
            decs.add(n)
        }
    }, decs)

    val ids = decs.flatMap { it.variables.map { it.nameAsString } }
    if(ids.distinct() != ids)
        return null

    return decs.find { it.variables.any { it.nameAsString == this.nameAsString} }?.commonType?.resolve()
}

internal fun MethodCallExpr.getNamespace(
    types: Map<String, IType>,
    foreignProcedures: List<IProcedureDeclaration>
): Namespace? =
    if (scope.isPresent) {
        val scope = scope.get()

        // local var resolve has to be first than class
        if(scope is NameExpr)
            scope.resolveLocalDeclaration()?.let {
                val static = if(it is ReferenceTypeImpl && it.typeDeclaration.isPresent)
                    it.typeDeclaration.get().declaredMethods.find { m ->
                        m.name == this.nameAsString
                    }?.isStatic ?: false
                else
                    false
                return Namespace(it.simpleNameAsString, false, static)
            }

        val scopeIsCurrentlyLoadedType: Boolean =
            scope is NameExpr && types.containsKey(scope.toString())
        val scopeIsCurrentlyLoadedForeignType: Boolean =
            foreignProcedures.any { it.namespace == scope.toString() }
        val scopeIsValidJavaClass: Boolean =
            isJavaClassName(scope.toString(), this)

        if (scopeIsCurrentlyLoadedType || scopeIsCurrentlyLoadedForeignType || scopeIsValidJavaClass)
            Namespace(scope.toString(), isAbstract = false, isStatic = true)
        else if (isAbstractMethodCall) when (val type =
            scope.getResolvedType()) {
            is ResolvedReferenceType -> Namespace(
                type.qualifiedName,
                isAbstract = true,
                isStatic = false
            )
            else -> null
        } else {
            // First try to load name from ITypes, and if that fails try as a Java type
            val name =
                runCatching { scope.getResolvedIType(types).id }.getOrNull()
                    ?: scope.getResolvedJavaType().canonicalName

            // Namespace $scope is not a class itself, but is it simplified for $name?
            val scopeIsSimplifiedName =
                name.split('.').last() == scope.toString()

            Namespace(
                name,
                isAbstract = false,
                isStatic = scopeIsSimplifiedName
            )
        }
    } else null
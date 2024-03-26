package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.strudel.javaparser.INIT
import pt.iscte.strudel.javaparser.stringType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.Value
import java.util.*
import kotlin.jvm.optionals.getOrDefault

fun getStringValue(str: String): IValue = Value(stringType, java.lang.String(str))

val IModule.proceduresExcludingConstructors: List<IProcedureDeclaration>
    get() = procedures.filter { it.id != INIT }

val <T> Optional<T>.getOrNull: T?
    get() =
        if (isPresent) this.get() else null

fun Optional<Comment>.translateComment(): String? =
    if (isPresent) {
        val comment = get()
        val str = comment.asString()
        str.substring(comment.header?.length ?: 0, str.length - (comment.footer?.length ?: 0)).trim()
    } else null

val CallableDeclaration<*>.body: BlockStmt?
    get() = if (this is ConstructorDeclaration) this.body else (this as MethodDeclaration).body.getOrNull

internal val MethodCallExpr.isAbstractMethodCall: Boolean
    get() = resolve().isAbstract

internal fun IProcedureDeclaration.matches(namespace: String?, id: String, parameterTypes: List<IType>): Boolean {
    val paramTypeMatch = this.parameters.map { it.type } == parameterTypes // FIXME
    val idAndNamespaceMatch =
        if (namespace == null) this.id == id
        else this.namespace == namespace && this.id == id
    return idAndNamespaceMatch // && paramTypeMatch
}

internal val ClassOrInterfaceDeclaration.qualifiedName: String
    get() = fullyQualifiedName.getOrDefault(nameAsString)

internal fun MethodDeclaration.replaceStringConcatPlus() {
    fun Expression.isStringType() = try {
        calculateResolvedType().describe() == "java.lang.String"
    } catch (e: Exception) {
        false
    }

    fun Expression.toStringExpression(): Expression {
        val type = calculateResolvedType()
        return if(this is CharLiteralExpr) StringLiteralExpr(value)
        else if(isLiteralExpr && this !is StringLiteralExpr) StringLiteralExpr(toString())
        else if(isNameExpr && type.isReferenceType) MethodCallExpr(this, "toString")
        else if(isNameExpr && type.isPrimitive)
            MethodCallExpr(NameExpr(type.asPrimitive().boxTypeClass.simpleName), SimpleName("toString"), NodeList(this))
        else this
    }

    val visitor = object : VoidVisitorAdapter<Any>() {
        override fun visit(n: BinaryExpr, arg: Any?) {
            if (n.operator == BinaryExpr.Operator.PLUS && (n.left.isStringType() || n.right.isStringType())) {
                val left = n.left.toStringExpression()
                val right = n.right.toStringExpression()
                val newCall = MethodCallExpr(left, "concat", NodeList.nodeList(right))
                n.replace(newCall)
                left.accept(this, arg)
                right.accept(this, arg)
            } else
                super.visit(n, arg)
        }
    }
    accept(visitor, null)
}
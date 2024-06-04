package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import pt.iscte.strudel.javaparser.INIT
import pt.iscte.strudel.javaparser.OUTER_PARAM
import pt.iscte.strudel.javaparser.StringType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.Reference
import pt.iscte.strudel.vm.impl.Value
import java.util.*
import kotlin.jvm.optionals.getOrDefault

val IProcedureDeclaration.outerParameter: IParameter
    get() = parameters.find { it.id == OUTER_PARAM }!!

val IProcedureDeclaration.hasOuterParameter: Boolean
    get() = kotlin.runCatching { this.outerParameter }.isSuccess

fun getString(value: String): IValue = Value(StringType, java.lang.String(value))

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
    get() = kotlin.runCatching { resolve().isAbstract }.getOrDefault(false)

internal fun IProcedureDeclaration.matches(namespace: String?, id: String, parameterTypes: List<IType>): Boolean {
    val paramTypeMatch = this.parameters.map { it.type } == parameterTypes // FIXME
    val idAndNamespaceMatch =
        if (namespace == null) this.id == id
        else this.namespace == namespace && this.id == id
    return idAndNamespaceMatch // && paramTypeMatch
}

internal val ClassOrInterfaceDeclaration.qualifiedName: String
    get() = fullyQualifiedName.getOrDefault(nameAsString)

internal fun VariableDeclarator.isGeneric(type: ClassOrInterfaceDeclaration): Boolean {
    return typeAsString in type.typeParameters.map { it.nameAsString } || (type.findAncestor(ClassOrInterfaceDeclaration::class.java).getOrNull?.let {
        isGeneric(
            it
        )
    } ?: false)
}

internal fun VariableDeclarator.getGenericDeclarator(): ClassOrInterfaceDeclaration? =
    this.findAncestor(
        { typeAsString in it.typeParameters.map { type -> type.nameAsString } },
        ClassOrInterfaceDeclaration::class.java
    ).getOrNull

internal fun MethodDeclaration.replaceStringConcatPlus() {
    fun Expression.isStringType(): Boolean = calculateResolvedType().describe() == "java.lang.String"

    fun Expression.toStringExpression(): Expression =
        if (this is CharLiteralExpr) StringLiteralExpr(value)
        else if (isLiteralExpr && this !is StringLiteralExpr) StringLiteralExpr(toString())
        else run {
            val type = calculateResolvedType()
            if (isNameExpr && type.isReferenceType) MethodCallExpr(this, "toString")
            else if (isNameExpr && type.isPrimitive) MethodCallExpr(
                NameExpr(type.asPrimitive().boxTypeClass.simpleName),
                SimpleName("toString"),
                NodeList(this)
            )
            else if (type.isPrimitive) MethodCallExpr(NameExpr("String"), "valueOf", NodeList.nodeList(this))
            else MethodCallExpr(this, "toString")
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


internal fun MethodDeclaration.replaceIncDecAsExpressions() {
    body.ifPresent { body ->
        body.findAll(UnaryExpr::class.java).filter { it.isIncrementOrDecrement }.forEach { u ->
            u.findAncestor(BlockStmt::class.java).ifPresent { block ->
                val injects = mutableListOf<Pair<Int, ExpressionStmt>>()

                block.statements.forEachIndexed { index, statement ->
                    statement.getSingleUnary().forEach { e ->
                        if (e.operator.isPrefix)
                            injects.add(Pair(index, ExpressionStmt(e.clone())))
                        else if (e.operator.isPostfix)
                            injects.add(Pair(index + 1, ExpressionStmt(e.clone())))
                        e.replace(e.expression)
                    }
                }

                injects.forEach {
                    block.statements.add(it.first, it.second)
                }
            }
        }
    }
}

internal val UnaryExpr.isIncrementOrDecrement: Boolean
    get() = operator == UnaryExpr.Operator.PREFIX_DECREMENT || operator == UnaryExpr.Operator.PREFIX_INCREMENT
            || operator == UnaryExpr.Operator.POSTFIX_DECREMENT || operator == UnaryExpr.Operator.POSTFIX_INCREMENT

internal fun Statement.getSingleUnary(): List<UnaryExpr> {

    val isCandidateForReplace = when (this) {
        is ExpressionStmt -> this.expression is AssignExpr || this.expression is MethodCallExpr
        is ReturnStmt -> true
        else -> false
    }

    return if (isCandidateForReplace)
        findAll(UnaryExpr::class.java).filter { u ->
            //val p = u.findAncestor({ it.parentNode.getOrNull !is Expression }, Expression::class.java)
            u.isIncrementOrDecrement && findAll(NameExpr::class.java).filter { n ->
                n == u.expression
            }.size == 1
        }
    else emptyList()
}

internal fun Node.substituteControlBlocks() {
    this.accept(object : VoidVisitorAdapter<Any>() {

        override fun visit(n: IfStmt, arg: Any?) {
            if (n.thenStmt !is BlockStmt) {
                // n.setThenStmt(if (n.thenStmt == null) BlockStmt() else BlockStmt(NodeList(n.thenStmt)))
                val thenStmt = n.thenStmt
                val block = BlockStmt()
                n.setThenStmt(block)
                if (thenStmt != null)
                    block.addStatement(thenStmt)
            }
            if (n.hasElseBranch() && n.elseStmt.get() !is BlockStmt) {
                // n.setElseStmt(BlockStmt(NodeList(n.elseStmt.get())))
                val elseStmt = n.elseStmt.get()
                val block = BlockStmt()
                n.setElseStmt(block)
                block.addStatement(elseStmt)
            }
            super.visit(n, arg)
        }

        override fun visit(n: WhileStmt, arg: Any?) {
            if (n.body !is BlockStmt) {
                // n.setBody(if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body)))
                val body = n.body
                val block = BlockStmt()
                n.setBody(block)
                if (body != null)
                    block.addStatement(body)
            }
            super.visit(n, arg)
        }

        override fun visit(n: DoStmt, arg: Any?) {
            if (n.body !is BlockStmt) {
                // n.setBody(if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body)))
                val body = n.body
                val block = BlockStmt()
                n.setBody(block)
                if (body != null)
                    block.addStatement(body)
            }
            super.visit(n, arg)
        }

        override fun visit(n: ForStmt, arg: Any?) {
            if (n.body !is BlockStmt) {
                // n.setBody(if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body)))
                val body = n.body
                val block = BlockStmt()
                n.setBody(block)
                if (body != null)
                    block.addStatement(body)
            }
            super.visit(n, arg)
        }

        override fun visit(n: ForEachStmt, arg: Any?) {
            if (n.body !is BlockStmt) {
                // n.setBody(if (n.body == null) BlockStmt() else BlockStmt(NodeList(n.body)))
                val body = n.body
                val block = BlockStmt()
                n.setBody(block)
                if (body != null)
                    block.addStatement(body)
            }
            super.visit(n, arg)
        }
    }, null)
}
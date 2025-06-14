package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.parsing.java.CONSTRUCTOR_FLAG
import pt.iscte.strudel.parsing.java.EQUALS_FLAG
import pt.iscte.strudel.parsing.java.OUTER_PARAM
import pt.iscte.strudel.parsing.java.StringType
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.impl.Reference
import pt.iscte.strudel.vm.impl.Value
import java.util.*
import kotlin.jvm.optionals.getOrDefault

val IProcedureDeclaration.outerParameter: IParameter
    get() = parameters.find { it.id == OUTER_PARAM }!!

val IProcedureDeclaration.hasOuterParameter: Boolean
    get() = kotlin.runCatching { this.outerParameter }.isSuccess

val IProcedureDeclaration.hasThisParameter: Boolean
    get() = kotlin.runCatching { this.thisParameter }.isSuccess

val IModule.proceduresExcludingConstructorsAndForeign: List<IProcedureDeclaration>
    get() = procedures.filter { !it.hasFlag(CONSTRUCTOR_FLAG) && !it.isForeign }

val IRecordType.hasEquals: Boolean
    get() = module?.let { m -> m.procedures.any { it.hasFlag(EQUALS_FLAG) } } == true

val IRecordType.equals: IProcedureDeclaration
    get() = module!!.procedures.first { it.hasFlag(EQUALS_FLAG) }

fun getString(value: String): IReference<*> = Reference(Value(StringType, value))

val <T> Optional<T>.getOrNull: T?
    get() = if (isPresent) this.get() else null

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
    val idAndNamespaceMatch =
        if (namespace == null) this.id == id
        else this.namespace == namespace && this.id == id
    if (!idAndNamespaceMatch) return false

    var paramTypes: List<IType> = this.parameters.map { it.type }
    if (this.hasOuterParameter)
        paramTypes = paramTypes.subList(1, paramTypes.size)
    if (this.hasThisParameter)
        paramTypes = paramTypes.subList(1, paramTypes.size)

    if (paramTypes.size != parameterTypes.size) return false

    return true
    /* FIXME
    val paramTypeMatch = paramTypes.zip(parameterTypes).all { it.first.isSame(it.second) }
    if (!paramTypeMatch)
        println("\tNope. Parameter types do not match.")
    return paramTypeMatch
     */
}

internal val TypeDeclaration<*>.qualifiedName: String
    get() = fullyQualifiedName.getOrDefault(nameAsString)

val TypeDeclaration<*>.nestedTypes: List<TypeDeclaration<*>>
    get() {
        val inner = members.filterIsInstance<TypeDeclaration<*>>()
        return if (inner.isEmpty()) listOf() else inner + inner.flatMap { it.nestedTypes }
    }

internal fun VariableDeclarator.isGeneric(type: ClassOrInterfaceDeclaration): Boolean =
    typeAsString in type.typeParameters.map { it.nameAsString } ||
            (type.findAncestor(ClassOrInterfaceDeclaration::class.java).getOrNull?.let { isGeneric(it) } == true)

internal fun Parameter.isGeneric(type: RecordDeclaration): Boolean =
    typeAsString in type.typeParameters.map { it.nameAsString } ||
            (type.findAncestor(RecordDeclaration::class.java).getOrNull?.let { isGeneric(it) } == true)

internal fun Node.replaceBinaryOperatorAssignWithRegularAssign() =
    accept(object : VoidVisitorAdapter<Any>() {
        override fun visit(n: AssignExpr, arg: Any?) {
            if (n.operator.toBinaryOperator().isPresent) {
                val target = n.target
                val value = n.value

                val binaryOperator = n.operator.toBinaryOperator().get()

                val newValue = BinaryExpr(target, value, binaryOperator)
                val newAssign = AssignExpr(target, newValue, AssignExpr.Operator.ASSIGN)

                n.replace(newAssign)
                newValue.accept(this, arg)

            } else
                super.visit(n, arg)
        }
    }, null)

internal fun BodyDeclaration<*>.replaceStringPlusWithConcat(types: MutableMap<String, IType>) {
    fun Expression.isStringType(): Boolean {
        val resolvedType = getResolvedType()
        return  resolvedType is ResolvedType && resolvedType.describe() == "java.lang.String"
    }
       // calculateResolvedType().describe() == "java.lang.String"

    fun Expression.toStringExpression(): Expression =
        if (this is CharLiteralExpr) StringLiteralExpr(value)
        else if (isLiteralExpr && this !is StringLiteralExpr) StringLiteralExpr(toString())
        else run {
            val type = getResolvedType()
            //val type = calculateResolvedType()
            if (isNameExpr && type.isReferenceType) MethodCallExpr(this, "toString")
            else if (isNameExpr && type.isPrimitive) MethodCallExpr(
                NameExpr(type.asPrimitive().boxTypeClass.name),
                SimpleName("toString"),
                NodeList(this)
            )
            else if (type.isPrimitive) MethodCallExpr(NameExpr("String"), "valueOf", NodeList.nodeList(this))
            else MethodCallExpr(this, "toString")
        }

    accept(object : VoidVisitorAdapter<Any>() {
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
    }, null)
}


internal fun BodyDeclaration<*>.replaceIncDecAsExpressions() {
    findAll(UnaryExpr::class.java).filter { it.isIncrementOrDecrement }.forEach { u ->
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
            u.isIncrementOrDecrement && findAll(NameExpr::class.java).filter { n -> n == u.expression }.size == 1
        }
    else emptyList()
}

internal fun Node.substituteControlBlocks() = accept(ControlStructureEncapsulateVisitor, null)

fun CompilationUnit.nodeAtLine(line: Int): Node? =
    findFirst(Node::class.java) { it.begin.isPresent && it.begin.get().line == line }.getOrNull
package pt.iscte.strudel.javaparser

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration
import pt.iscte.strudel.javaparser.extensions.getOrNull
import pt.iscte.strudel.javaparser.extensions.mapType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.False
import pt.iscte.strudel.model.dsl.True
import pt.iscte.strudel.model.dsl.character
import pt.iscte.strudel.model.dsl.lit
import pt.iscte.strudel.model.impl.Conditional
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.UnaryOperator

class JavaExpression2Strudel(
    val procedure: IProcedure,
    val block: IBlock,
    val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
    private val types: MutableMap<String, IType>,
    private val translator: Java2Strudel,
    val decMap: MutableMap<VariableDeclarator, IVariableDeclaration<IBlock>>

) {

    private fun findVariableResolve(v: NameExpr): IVariableDeclaration<*>? {
        fun findVariable(id: String): IVariableDeclaration<*>? =
            procedure.variables.find { it.id == id } ?: procedure.parameters.find { it.id == THIS_PARAM }?.let { p ->
                ((p.type as IReferenceType).target as IRecordType).getField(id)
            }
        return when (val r = v.resolve()) {
            is JavaParserVariableDeclaration -> decMap[r.variableDeclarator]
            is JavaParserParameterDeclaration -> findVariable(r.wrappedNode.nameAsString)
            is JavaParserFieldDeclaration -> findVariable(r.name)
            else -> null
        }
    }

    fun map(exp: Expression): IExpression = with(translator) {
        when (exp) {
            is IntegerLiteralExpr -> lit(exp.value.toInt())
            is DoubleLiteralExpr -> lit(exp.value.toDouble())
            is CharLiteralExpr -> character(exp.value[0])
            is BooleanLiteralExpr -> if (exp.value) True else False

            is NameExpr -> {
                val target = findVariableResolve(exp) //findVariable(exp.nameAsString)
                if (target?.isField == true) procedure.thisParameter.field(target as IVariableDeclaration<IRecordType>)
                else target?.expression() ?: error("not found $exp", exp)
            }

            is ThisExpr -> {
                procedure.thisParameter.expression()
            }

            is NullLiteralExpr -> NULL_LITERAL

            is StringLiteralExpr -> {
                translator.foreignProcedures.find { it.id == NEW_STRING }!!.expression(
                    CHAR.array().heapAllocationWith(exp.value.map {
                        CHAR.literal(it)
                    })
                )
            }

            is UnaryExpr -> mapUnaryOperator(exp).on(map(exp.expression)).apply {
                // TODO review
                val from = exp.range.get().begin.column
                val to = exp.expression.range.get().begin.column - 1
                setProperty(
                    OPERATOR_LOC, SourceLocation(
                        exp.expression.range.get().begin.line, from, to
                    )
                )
            }

            is BinaryExpr ->
                // short circuit &&
                if (exp.operator == BinaryExpr.Operator.AND)
                    Conditional(map(exp.left), map(exp.right), False)
                // short circuit ||
                else if (exp.operator == BinaryExpr.Operator.OR)
                    Conditional(map(exp.left), True, map(exp.right))
                else
                    mapBinaryOperator(exp).on(
                        map(exp.left), map(exp.right)
                    ).apply {
                        if (exp.left.range.isPresent && exp.right.range.isPresent && exp.left.range.get().begin.line == exp.right.range.get().begin.line) {
                            val from = exp.left.range.get().end.column + 1
                            val to = exp.right.range.get().begin.column - 1
                            setProperty(
                                OPERATOR_LOC, SourceLocation(
                                    exp.left.range.get().begin.line, from, to
                                )
                            )
                        }
                    }

            is EnclosedExpr -> map(exp.inner)

            is CastExpr -> UnaryOperator.TRUNCATE.on(map(exp.expression)) //TODO other casts

            // TODO multi level
            is ArrayCreationExpr -> {
                if (exp.levels.any { !it.dimension.isPresent }) unsupported(
                    "multi-dimension array initialization with partial dimensions",
                    exp
                )

                val arrayType = types.mapType(exp.elementType.asString()).array()

                if (exp.levels[0].dimension.isPresent) arrayType.heapAllocation(exp.levels.map {
                    map(
                        it.dimension.get()
                    )
                })
                else map(exp.initializer.get())
            }

            is ArrayInitializerExpr -> {
                val values = exp.values.map { map(it) }
                val baseType =
                    if (exp.parentNode.getOrNull is ArrayCreationExpr) types.mapType((exp.parentNode.get() as ArrayCreationExpr).elementType)
                    else if (exp.parentNode.getOrNull is VariableDeclarator) types.mapType((exp.parentNode.get() as VariableDeclarator).typeAsString)
                    else unsupported("array initializer", exp)

                baseType.asArrayType.heapAllocationWith(values)
            }

            is ArrayAccessExpr -> map(exp.name).element(map(exp.index))

            is ObjectCreationExpr -> {
                val const = procedures.findProcedure(
                    exp.type.nameAsString, INIT, emptyList()
                ) // TODO params
                    ?: unsupported("constructor for type ${exp.type.nameWithScope}", exp)
                val alloc = types.mapType(exp.type).asRecordType.heapAllocation()
                const.expression(listOf(alloc) + exp.arguments.map { map(it) })
            }

            is FieldAccessExpr -> {
                if (exp.scope is ArrayAccessExpr && exp.nameAsString == "length") {
                    map(exp.scope).length()
                } else {
                    if (exp.scope is ThisExpr) {
                        val thisParam = procedure.parameters.find { it.id == THIS_PARAM }!!
                        val thisType = (thisParam.type as IReferenceType).target as IRecordType
                        thisParam.field(thisType.getField(exp.nameAsString)!!)
                    } else {
                        val solve = JPFacade.solve(exp.scope)
                        val type = solve.correspondingDeclaration.type
                        val typeId = type.describe()
                        if (type.isArray && exp.nameAsString == "length") map(exp.scope).length()
                        else {
                            val f = types[typeId]?.asRecordType?.fields?.find { it.id == exp.nameAsString } ?: error(
                                "not found $exp", exp
                            ) // UnboundVariableDeclaration(exp.nameAsString, procedure)

                            map(exp.scope).field(f)
                        }
                    }
                }
            }

            is MethodCallExpr -> translator.handleMethodCall(procedure, procedures, types, exp, ::map) { m, args ->
                ProcedureCall(NullBlock, m, arguments = args)
            }

            is ConditionalExpr -> Conditional(map(exp.condition), map(exp.thenExpr), map(exp.elseExpr))

            else -> unsupported("expression type ${exp::class.simpleName}", exp)
        }.bind(exp)
    }

    private fun isStringConcat(exp: BinaryExpr): Boolean {
        if(exp.operator != BinaryExpr.Operator.PLUS)
            return false

        val leftType = exp.left.calculateResolvedType()
        val rightType = exp.right.calculateResolvedType()

        return (leftType is ResolvedReferenceType && leftType.qualifiedName == String::class.java.name) //|| rightType.isString
    }
}


fun AssignExpr.Operator.map(a: AssignExpr): IBinaryOperator =
    when (this) {
        AssignExpr.Operator.PLUS -> ArithmeticOperator.ADD
        AssignExpr.Operator.MINUS -> ArithmeticOperator.SUB
        AssignExpr.Operator.MULTIPLY -> ArithmeticOperator.MUL
        AssignExpr.Operator.DIVIDE -> ArithmeticOperator.DIV
        AssignExpr.Operator.REMAINDER -> ArithmeticOperator.MOD
        else -> unsupported("assign operator", a)
    }

fun mapUnaryOperator(exp: UnaryExpr): IUnaryOperator = when (exp.operator) {
    UnaryExpr.Operator.LOGICAL_COMPLEMENT -> UnaryOperator.NOT
    UnaryExpr.Operator.PLUS -> UnaryOperator.PLUS
    UnaryExpr.Operator.MINUS -> UnaryOperator.MINUS
    else -> unsupported("unary operator", exp)
}

fun mapBinaryOperator(exp: BinaryExpr): IBinaryOperator = when (exp.operator) {
    BinaryExpr.Operator.PLUS -> ArithmeticOperator.ADD
    BinaryExpr.Operator.MINUS -> ArithmeticOperator.SUB
    BinaryExpr.Operator.MULTIPLY -> ArithmeticOperator.MUL
    BinaryExpr.Operator.DIVIDE -> ArithmeticOperator.DIV
    BinaryExpr.Operator.REMAINDER -> ArithmeticOperator.MOD

    // TODO IDIV
    // BinaryExpr.Operator.DIVIDE -> ArithmeticOperator.IDIV

    BinaryExpr.Operator.EQUALS -> RelationalOperator.EQUAL
    BinaryExpr.Operator.NOT_EQUALS -> RelationalOperator.DIFFERENT
    BinaryExpr.Operator.LESS -> RelationalOperator.SMALLER
    BinaryExpr.Operator.LESS_EQUALS -> RelationalOperator.SMALLER_EQUAL
    BinaryExpr.Operator.GREATER -> RelationalOperator.GREATER
    BinaryExpr.Operator.GREATER_EQUALS -> RelationalOperator.GREATER_EQUAL

    BinaryExpr.Operator.BINARY_AND -> LogicalOperator.AND
    BinaryExpr.Operator.BINARY_OR -> LogicalOperator.OR

    BinaryExpr.Operator.XOR -> LogicalOperator.XOR

    else -> unsupported("binary operator", exp)
}

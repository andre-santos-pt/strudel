package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.Conditional
import pt.iscte.strudel.model.impl.Literal
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.UnaryOperator
import pt.iscte.strudel.parsing.java.extensions.*

class JavaExpression2Strudel(
    val procedure: IProcedure,
    val block: IBlock,
    val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
    private val types: MutableMap<String, IType>,
    private val translator: Java2Strudel,
    private val variableDeclarationMap: MutableMap<VariableDeclarator, IVariableDeclaration<IBlock>>
) {

    private fun IRecordType.findFieldInHierarchy(id: String): IField? =
        getField(id) ?: if (declaringType != null) declaringType!!.findFieldInHierarchy(id) else null

    // Finds the variable for a given ID
    fun findVariable(id: String): IVariableDeclaration<*>? =
        procedure.variables.find { it.id == id } ?: procedure.parameters.find { it.id == THIS_PARAM }?.let { p ->
            ((p.type as IReferenceType).target as IRecordType).findFieldInHierarchy(id)
        }

    // Finds the variable declaration for a given variable name expression
    private fun findVariableResolve(v: NameExpr): IVariableDeclaration<*>? =
        kotlin.runCatching { when (val r = v.resolve()) {
            is JavaParserVariableDeclaration -> variableDeclarationMap[r.variableDeclarator]
            is JavaParserParameterDeclaration -> findVariable(r.wrappedNode.nameAsString)
            is JavaParserFieldDeclaration -> findVariable(r.name)
            else -> null
        } }.getOrNull()

    fun map(exp: Expression): IExpression = with(translator) {
        when (exp) {
            is IntegerLiteralExpr -> lit(exp.value.toInt())
            is DoubleLiteralExpr -> lit(exp.value.toDouble())
            is CharLiteralExpr -> character(exp.value[0])
            is BooleanLiteralExpr -> if (exp.value) True else False

            is NameExpr -> {
                val target = findVariableResolve(exp) ?: findVariable(exp.nameAsString)
                if (target == null)
                    System.err.println("Could not find variable for expression ${exp.parentNode.getOrNull ?: exp}")
                if (target?.isField == true) {
                    val fieldOwnerType = target.owner as IRecordType
                    val procedureDeclaringType = procedure.thisParameter.type.asRecordType
                    val outerType = procedureDeclaringType.declaringType
                    if (outerType != null && outerType.isSame(fieldOwnerType))
                        if (procedure.hasOuterParameter) procedure.outerParameter.field(target as IField)
                        else procedureDeclaringType.getField(OUTER_PARAM)!!.field(target as IField)
                    else if (procedureDeclaringType.isSame(fieldOwnerType))
                        procedure.thisParameter.field(target as IField)
                    else
                        unsupported("inner class variable more than 1 nested types deep", exp)
                }
                else target?.expression() ?: error("not found $exp", exp)
            }

            is ThisExpr -> procedure.thisParameter.expression()

            is NullLiteralExpr -> NULL_LITERAL

            is StringLiteralExpr -> translator.foreignProcedures.find { it.id == NEW_STRING }!!.expression(
                CHAR.array().heapAllocationWith(exp.value.map {
                    CHAR.literal(it)
                })
            )

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

            is BinaryExpr -> when (exp.operator) {
                BinaryExpr.Operator.AND -> Conditional(map(exp.left), map(exp.right), False) // short-circuit &&
                BinaryExpr.Operator.OR -> Conditional(map(exp.left), True, map(exp.right)) // short-circuit ||
                else -> mapBinaryOperator(exp).on(map(exp.left), map(exp.right)).apply {
                    if (exp.left.range.isPresent && exp.right.range.isPresent) {
                        val leftRange = exp.left.range.get()
                        val rightRange = exp.right.range.get()
                        if (leftRange.begin.line == rightRange.begin.line) {
                            val from = leftRange.end.column + 1
                            val to = rightRange.begin.column - 1
                            setProperty(OPERATOR_LOC, SourceLocation(leftRange.begin.line, from, to))
                        }
                    }
                }
            }

            is EnclosedExpr -> map(exp.inner)

            is CastExpr -> when (val t = exp.type) {
                is PrimitiveType -> when (val p = exp.type.asPrimitiveType().type) {
                    PrimitiveType.Primitive.INT -> UnaryOperator.CAST_TO_INT.on(map(exp.expression))
                    PrimitiveType.Primitive.DOUBLE -> UnaryOperator.CAST_TO_DOUBLE.on(map(exp.expression))
                    PrimitiveType.Primitive.CHAR -> UnaryOperator.CAST_TO_CHAR.on(map(exp.expression))
                    else -> unsupported("cast to primitive type ${p.name}", exp)
                }
                else -> unsupported("cast to non-primitive type $t", exp)
            }

            // TODO multi level
            is ArrayCreationExpr -> {
                if (exp.levels.any { !it.dimension.isPresent })
                    unsupported("multi-dimension array initialization with partial dimensions", exp)

                val arrayType = types.mapType(exp.elementType).array()

                if (exp.levels[0].dimension.isPresent)
                    arrayType.heapAllocation(exp.levels.map { map(it.dimension.get()) })
                else map(exp.initializer.get())
            }

            is ArrayInitializerExpr -> {
                val values = exp.values.map { map(it) }
                val baseType = when (val parent = exp.parentNode.getOrNull) {
                    is ArrayCreationExpr -> types.mapType(parent.elementType)
                    is VariableDeclarator -> types.mapType(parent.type)
                    else -> unsupported("array initializer", exp)
                }
                baseType.asArrayType.heapAllocationWith(values)
            }

            is ArrayAccessExpr -> map(exp.name).element(map(exp.index))

            is ObjectCreationExpr -> {
                val paramTypes = exp.arguments.map { it.getResolvedIType(types) }
                val const: IProcedureDeclaration =
                    kotlin.runCatching { procedures.findProcedure(exp.type.resolve().describe(), INIT, paramTypes) }.getOrNull()
                    ?: procedures.findProcedure(exp.type.nameAsString, INIT, paramTypes)
                    ?: exp.asForeignProcedure(procedure.module!!, types)
                val alloc = types.mapType(exp.type).asRecordType.heapAllocation()
                if (const.hasOuterParameter)
                    const.expression(listOf(procedure.thisParameter.exp(), alloc) + exp.arguments.map { map(it) })
                else
                    const.expression(listOf(alloc) + exp.arguments.map { map(it) })
            }

            is FieldAccessExpr -> {
                if (exp.scope is ArrayAccessExpr && exp.nameAsString == "length")
                    map(exp.scope).length()
                else {
                    if (exp.scope is ThisExpr) {
                        val thisParam = procedure.parameters.find { it.id == THIS_PARAM }!!
                        val thisType = (thisParam.type as IReferenceType).target as IRecordType
                        thisParam.field(thisType.getField(exp.nameAsString)!!)
                    } else {
                        val type = kotlin.runCatching { JPFacade.solve(exp.scope).correspondingDeclaration.type }.getOrDefault(exp.scope.calculateResolvedType())
                        val typeId = type.describe()
                        val isJavaStatic = "$typeId.${exp.nameAsString}".endsWith(exp.toString())

                        if (type.isArray && exp.nameAsString == "length") map(exp.scope).length()
                        else if (isJavaClassName(typeId) && isJavaStatic) { // Foreign field is translated to foreign getter procedure :)
                            ProcedureCall(
                                NullBlock,
                                type.foreignStaticFieldAccess(procedure.module!!, types),
                                arguments = listOf(Literal(StringType, exp.nameAsString))
                            )
                        }
                        else {
                            val f = types[typeId]?.asRecordType?.fields?.find { it.id == exp.nameAsString } ?: error(
                                "could not find field \"${exp.nameAsString}\" within record type $typeId", exp
                            )
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

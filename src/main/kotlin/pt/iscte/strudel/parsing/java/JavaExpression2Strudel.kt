package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
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
import pt.iscte.strudel.vm.impl.ForeignProcedure

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
    private fun findVariable(id: String): IVariableDeclaration<*>? =
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

    // Translates an expression, but automatically casts character expressions to integers.
    // Used in binary expression which use characters.
    private fun Expression.toCharCodeOrDefault(): IExpression = kotlin.runCatching {
        val type = this.getResolvedType()
        if (type == ResolvedPrimitiveType.CHAR) UnaryOperator.CAST_TO_INT.on(map(this))
        else map(this)
    }.getOrDefault(map(this))

    private fun IBinaryOperator.onCharCodeOrDefault(left: Expression, right: Expression): IExpression =
        kotlin.runCatching {
            val leftType = left.getResolvedType()
            val rightType = right.getResolvedType()
            if (this == RelationalOperator.EQUAL && leftType == ResolvedPrimitiveType.CHAR && rightType == ResolvedPrimitiveType.CHAR)
                on(map(left), map(right))
            else on(left.toCharCodeOrDefault(), right.toCharCodeOrDefault())
        }.getOrDefault(on(left.toCharCodeOrDefault(), right.toCharCodeOrDefault()))

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
                        LoadingError.unsupported("inner class variable more than 1 nested types deep", exp)
                }
                else target?.expression() ?: LoadingError.translation("not found $exp", exp)
            }

            is ThisExpr -> procedure.thisParameter.expression()

            is NullLiteralExpr -> NULL_LITERAL

            is StringLiteralExpr -> {
                val content = exp.value
                    .replace("\\n","\n")
                    .replace("\\t","\t")
                    .replace("\\\"","\"")
                    .replace("\\\\", "\\")
                translator.foreignProcedures.find { it.id == NEW_STRING }!!
                    .expression(
                        CHAR.array()
                            .heapAllocationWith(content.map { CHAR.literal(it) })
                    )
            }
            is UnaryExpr -> mapUnaryOperator(exp).on(exp.expression.toCharCodeOrDefault()).apply {
                // TODO review
                val from = exp.range.get().begin.column
                val to = exp.expression.range.get().end.column - 1
                val len = exp.expression.tokenRange.get().sumOf { it.text.length }
                setProperty(
                    OPERATOR_LOC,
                    SourceLocation(exp.expression.range.get().begin.line, exp.expression.range.get().end.line, from, to, len)
                )
            }

            is BinaryExpr -> when (exp.operator) {
                BinaryExpr.Operator.AND -> Conditional(map(exp.left), map(exp.right), False) // short-circuit &&
                BinaryExpr.Operator.OR -> Conditional(map(exp.left), True, map(exp.right)) // short-circuit ||
                else -> {

                    mapBinaryOperator(exp).onCharCodeOrDefault(exp.left, exp.right).apply {
                        if (exp.left.range.isPresent && exp.right.range.isPresent) {
                            val leftRange = exp.left.range.get()
                            val rightRange = exp.right.range.get()
                            if (leftRange.begin.line == rightRange.begin.line) {
                                val from = leftRange.end.column + 1
                                val to = rightRange.begin.column - 1
                                setProperty(OPERATOR_LOC, SourceLocation(leftRange.begin.line, rightRange.end.line, from, to, to - from))
                            }
                        }
                    }
                }
            }

            is EnclosedExpr -> map(exp.inner)

            is CastExpr -> when (val t = exp.type) {
                is PrimitiveType -> when (val p = t.asPrimitiveType().type) {
                    PrimitiveType.Primitive.INT -> UnaryOperator.CAST_TO_INT.on(map(exp.expression))
                    PrimitiveType.Primitive.DOUBLE -> UnaryOperator.CAST_TO_DOUBLE.on(map(exp.expression))
                    PrimitiveType.Primitive.CHAR -> UnaryOperator.CAST_TO_CHAR.on(map(exp.expression))
                    else -> LoadingError.unsupported("cast to primitive type ${p.name}", exp)
                }
                else -> LoadingError.unsupported("cast to non-primitive type $t", exp)
            }

            // TODO multi level
            is ArrayCreationExpr -> {
                if (exp.levels.size > 1 && exp.levels.any { !it.dimension.isPresent })
                    LoadingError.unsupported("multi-dimension array initialization with partial dimensions", exp)

                val arrayType = types.mapType(exp.elementType, exp).array()

                if (exp.levels[0].dimension.isPresent)
                    arrayType.heapAllocation(exp.levels.map { map(it.dimension.get()) })
                else
                    map(exp.initializer.get())
            }

            is ArrayInitializerExpr -> {
                val values = exp.values.map { map(it) }
                val baseType = when (val parent = exp.parentNode.getOrNull) {
                    is ArrayCreationExpr -> types.mapType(parent.elementType, exp).array()
                    is VariableDeclarator -> types.mapType(parent.type, exp)
                    is ArrayInitializerExpr -> {
                        try {
                            val type = (exp.parentNode.get().parentNode.get() as VariableDeclarator).type as ArrayType
                            types.mapType(type.elementType, exp).array()
                        }
                        catch (e: Exception) {
                            LoadingError.unsupported("array initializer", exp)
                        }
                    }
                    else -> LoadingError.unsupported("array initializer", exp)
                }
                baseType.asArrayType.heapAllocationWith(values)
            }

            is ArrayAccessExpr -> map(exp.name).element(map(exp.index))

            is ObjectCreationExpr -> {
                val paramTypes = exp.arguments.map { it.getResolvedIType(types) }
                val const: IProcedureDeclaration =
                    procedures.findProcedure(exp.type.nameAsString, INIT, paramTypes)
                    ?: kotlin.runCatching { procedures.findProcedure(exp.type.resolve().simpleNameAsString, INIT, paramTypes) }.getOrNull()
                    ?: exp.asForeignProcedure(procedure.module!!, types)
                if(const is ForeignProcedure) {
                    const.expression(exp.arguments.map { map(it) })
                }
                else {
                    val alloc = types.mapType(
                        exp.type,
                        exp
                    ).asRecordType.heapAllocation()
                    if (const.hasOuterParameter)
                        const.expression(
                            listOf(
                                procedure.thisParameter.exp(),
                                alloc
                            ) + exp.arguments.map { map(it) })
                    else
                        const.expression(listOf(alloc) + exp.arguments.map {
                            map(
                                it
                            )
                        })
                }
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
                        val type = kotlin.runCatching { JPFacade.solve(exp.scope).correspondingDeclaration.type }.getOrDefault(exp.scope.getResolvedType())
                        val typeId = type.simpleNameAsString
                        val isJavaStatic = "$typeId.${exp.nameAsString}".endsWith(exp.toString())

                        if (type.isArray && exp.nameAsString == "length") map(exp.scope).length()
                        else if (isJavaClassName(typeId, exp.scope) && isJavaStatic) { // Foreign field is translated to foreign getter procedure :)
                            ProcedureCall(
                                NullBlock,
                                type.foreignStaticFieldAccess(procedure.module!!, types, exp.scope),
                                arguments = listOf(Literal(StringType, exp.nameAsString))
                            )
                        }
                        else {
                            val f = types[typeId]?.asRecordType?.fields?.find { it.id == exp.nameAsString } ?:
                            LoadingError.translation(
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

            else -> LoadingError.unsupported("expression type ${exp::class.simpleName}", exp)
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
        else -> LoadingError.unsupported("assign operator", a)
    }

fun mapUnaryOperator(exp: UnaryExpr): IUnaryOperator = when (exp.operator) {
    UnaryExpr.Operator.LOGICAL_COMPLEMENT -> UnaryOperator.NOT
    UnaryExpr.Operator.PLUS -> UnaryOperator.PLUS
    UnaryExpr.Operator.MINUS -> UnaryOperator.MINUS
    else -> LoadingError.unsupported("unary operator", exp)
}

private fun BinaryExpr.isIntegerArithmetic(): Boolean =
    left.getResolvedType() == ResolvedPrimitiveType.INT && right.getResolvedType() == ResolvedPrimitiveType.INT

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

    BinaryExpr.Operator.BINARY_AND ->
        if (exp.isIntegerArithmetic()) ArithmeticOperator.BITWISE_AND
        else LogicalOperator.AND

    BinaryExpr.Operator.BINARY_OR ->
        if (exp.isIntegerArithmetic()) ArithmeticOperator.BITWISE_OR
        else LogicalOperator.OR

    BinaryExpr.Operator.XOR ->
        if (exp.isIntegerArithmetic()) ArithmeticOperator.BITWISE_XOR
        else LogicalOperator.XOR

    BinaryExpr.Operator.LEFT_SHIFT -> ArithmeticOperator.LEFT_SHIFT
    BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> ArithmeticOperator.SIGNED_RIGHT_SHIFT
    BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> ArithmeticOperator.UNSIGNED_RIGHT_SHIFT

    else -> LoadingError.unsupported("binary operator", exp)
}

package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.impl.RecordFieldExpression
import pt.iscte.strudel.model.impl.VariableExpression
import pt.iscte.strudel.model.util.UnaryOperator
import pt.iscte.strudel.parsing.java.extensions.getTypeFromJavaParser
import pt.iscte.strudel.parsing.java.extensions.mapType
import pt.iscte.strudel.parsing.java.extensions.translateComment

class JavaStatement2Strudel(
    val procedure: IProcedure,
    val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
    val types: MutableMap<String, IType>,
    private val translator: Java2Strudel
) {

    private val decMap = mutableMapOf<VariableDeclarator, IVariableDeclaration<IBlock>>()

    fun translate(stmt: Statement, block: IBlock) {
        with(translator) {
            val exp2Strudel = JavaExpression2Strudel(procedure, block, procedures, types, translator, decMap)

            fun findVariable(id: String): IVariableDeclaration<*>? =
                procedure.variables.find { it.id == id } ?:
                procedure.parameters.find { it.id == THIS_PARAM }?.
                let { p -> ((p.type as IReferenceType).target as IRecordType).getField(id) }

            fun FieldAccessExpr.findBaseScope(): Expression = when(scope) {
                is NameExpr -> scope
                is FieldAccessExpr -> (scope as FieldAccessExpr).findBaseScope()
                else -> unsupported("field access", stmt)
            }

            fun handleAssign(block: IBlock, a: AssignExpr): IStatement {
                // NameExpr
                fun handleNameExpr(name: NameExpr): IStatement {
                    val target = findVariable(name.toString()) ?: error("not found", a)
                    val v = when (a.operator) {
                        AssignExpr.Operator.ASSIGN -> exp2Strudel.map(a.value)
                        else -> a.operator.map(a).on(exp2Strudel.map(name), exp2Strudel.map(a.value))
                    }
                    return if (target.isField)
                        block.FieldSet(
                            procedure.thisParameter.expression(),
                            target as IVariableDeclaration<IRecordType>,
                            v
                        )
                    else block.Assign(target, v)
                }

                // ArrayAccessExpr
                fun handleArrayAccessExpr(arrayAccess: ArrayAccessExpr): IStatement {
                    return when (a.operator) { // TODO compound assignment operators
                        AssignExpr.Operator.ASSIGN ->
                            block.ArraySet(
                                exp2Strudel.map(arrayAccess.name) as ITargetExpression,
                                exp2Strudel.map(arrayAccess.index),
                                exp2Strudel.map(a.value)
                            )
                        else -> unsupported("assign operator ${a.operator}", a)
                    }
                }

                // FieldAccessExpr
                fun handleFieldAccessExpr(fieldAccessExpr: FieldAccessExpr): IStatement {
                    val typeId =
                        if (fieldAccessExpr.scope.isThisExpr) procedure.namespace
                        else JPFacade.solve(fieldAccessExpr.findBaseScope()).correspondingDeclaration.type.asReferenceType().id
                    return when (a.operator) { // TODO compound assignment operators
                        AssignExpr.Operator.ASSIGN ->
                            block.FieldSet(
                                exp2Strudel.map(fieldAccessExpr.scope) as ITargetExpression,
                                types[typeId]?.asRecordType?.fields?.find { it.id == fieldAccessExpr.nameAsString }
                                    ?: unsupported("field access", a),
                                exp2Strudel.map(a.value)
                            )
                        else -> unsupported("assign operator ${a.operator}", a)
                    }
                }

                return when (a.target) {
                    is NameExpr -> handleNameExpr(a.target as NameExpr)
                    is ArrayAccessExpr -> handleArrayAccessExpr(a.target as ArrayAccessExpr)
                    is FieldAccessExpr -> handleFieldAccessExpr(a.target as FieldAccessExpr)
                    else -> unsupported("assignment", a)
                }
            }

            fun handleExpression(block: IBlock, s: Expression): IStatement =
                when (s) {
                    is AssignExpr -> handleAssign(block, s)

                    is UnaryExpr -> when(s.operator) {
                        UnaryExpr.Operator.PREFIX_INCREMENT, UnaryExpr.Operator.POSTFIX_INCREMENT ->
                            when (val varExp = exp2Strudel.map(s.expression)) {
                                is VariableExpression -> block.Assign(varExp.variable, varExp + lit(1))
                                is RecordFieldExpression -> block.FieldSet(
                                    varExp.target,
                                    varExp.field,
                                    varExp + lit(1)
                                )
                                else -> unsupported("${s.operator} on ${s.expression}", s)
                            }
                        UnaryExpr.Operator.PREFIX_DECREMENT, UnaryExpr.Operator.POSTFIX_DECREMENT ->
                            when (val varExp = exp2Strudel.map(s.expression)) {
                                is VariableExpression -> block.Assign(varExp.variable, varExp - lit(1))
                                is RecordFieldExpression -> block.FieldSet(
                                    varExp.target,
                                    varExp.field,
                                    varExp - lit(1)
                                )
                                else -> unsupported("${s.operator} on ${s.expression}", s)
                            }
                        else -> unsupported("unary expression statement", s)
                    }
                    is MethodCallExpr -> {
                        translator.handleMethodCall(procedure, procedures, types, s, exp2Strudel::map) { m, args ->
                            ProcedureCall(block, m, arguments = args)
                        }
                    }
                    else -> unsupported("expression statement", s)
                }

            if (stmt is EmptyStmt) return
            else if (stmt is BlockStmt) {
                stmt.statements.forEach { translate(it, block) }
                return
            }

            // ReturnStmt
            fun handleReturnStmt(stmt: ReturnStmt) {
                val statement =
                    if (stmt.expression.isPresent)
                        block.Return(exp2Strudel.map(stmt.expression.get())).apply { setFlag() }
                    else
                        block.ReturnVoid()
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // IfStmt
            fun handleIfStmt(stmt: IfStmt) {
                val statement = block.If(exp2Strudel.map(stmt.condition)) {
                    translate(stmt.thenStmt, this)
                }.apply {
                    if (stmt.hasElseBranch())
                        createAlternativeBlock { translate(stmt.elseStmt.get(), it) }
                }
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // WhileStmt
            fun handleWhileStmt(stmt: WhileStmt) {
                val statement = block.While(exp2Strudel.map(stmt.condition)) { translate(stmt.body, this) }
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // ForStmt
            fun handleForStmt(stmt: ForStmt) {
                val statement = block.Block(FOR) {
                    stmt.initialization.forEach { i ->
                        if (i.isVariableDeclarationExpr) {
                            i.asVariableDeclarationExpr().variables.forEach { v ->
                                val varDec = addVariable(types.mapType(v.type, v)).apply {
                                    id = v.nameAsString
                                    setFlag(FOR)
                                    bind(i)
                                    bind(v.name, ID_LOC)
                                    bind(v.type, TYPE_LOC)
                                }
                                decMap[v] = varDec
                                if (v.initializer.isPresent) Assign(
                                    varDec,
                                    exp2Strudel.map(v.initializer.get())
                                ).apply {
                                    setFlag(FOR)
                                    bind(v)
                                }
                            }
                        } else handleExpression(this, i).bind(i)
                    }
                    val guard = if (stmt.compare.isPresent) exp2Strudel.map(stmt.compare.get()) else True
                    While(guard) {
                        translate(stmt.body, this)
                        stmt.update.forEach { u ->
                            handleExpression(this, u).apply {
                                setFlag(FOR)
                                bind(u)
                            }
                        }
                    }.apply {
                        setFlag(FOR)
                        bind(stmt)
                    }
                }
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // ForEachStmt
            fun handleForEachStmt(stmt: ForEachStmt) {
                val statement = block.Block(EFOR) {
                    stmt.variable.variables.forEach { dec ->
                        val itVar =
                            addVariable(types.mapType(stmt.variable.elementType, stmt.variable)).apply {
                                id = dec.nameAsString
                                setFlag(EFOR)
                                bind(stmt.variable)
                                bind(dec.name, ID_LOC)
                                bind(dec.type, TYPE_LOC)
                            }
                        val indexVar = addVariable(INT).apply {
                            id = IT + this@Block.depth
                            setFlag(EFOR)
                            bind(stmt.variable)
                        }
                        Assign(indexVar, lit(0)).apply { setFlag(EFOR) }
                        While(indexVar.expression() smaller exp2Strudel.map(stmt.iterable).length()) {
                            Assign(
                                itVar,
                                exp2Strudel.map(stmt.iterable).element(indexVar.expression())
                            ).apply {
                                setFlag(EFOR)
                            }
                            translate(stmt.body, this)
                            Assign(
                                indexVar,
                                indexVar.expression() plus lit(1)
                            ).apply {
                                setFlag(EFOR)
                            }
                        }.apply {
                            setFlag(EFOR)
                        }
                    }
                }
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // ExpressionStmt
            fun handleExpressionStmt(stmt: ExpressionStmt) {
                if (stmt.expression is VariableDeclarationExpr) {
                    (stmt.expression as VariableDeclarationExpr).variables.forEach { dec ->
                        val type = getTypeFromJavaParser(stmt, dec.type, types, stmt.expression)
                        val s: IVariableDeclaration<IBlock> = run {
                            val varDec = block.Var(type, dec.nameAsString)
                            decMap[dec] = varDec
                            if (dec.initializer.isPresent)
                                block.Assign(varDec, exp2Strudel.map(dec.initializer.get())).bind(stmt)
                            varDec.bind(dec.type, TYPE_LOC).bind(dec.name, ID_LOC)
                        }
                        stmt.comment.translateComment()?.let { s.documentation = it }
                        s.bind(stmt)
                    }
                }
                else {
                    val statement = handleExpression(block, stmt.expression)
                    stmt.comment.translateComment()?.let { statement.documentation = it }
                    statement.bind(stmt)
                }
            }

            // ThrowStmt
            fun handleThrowStmt(stmt: ThrowStmt) {
                // throw statement only supported if expression is object creation with single String argument
                val exc = stmt.expression as? ObjectCreationExpr
                val args = exc?.arguments
                val statement =
                    if (exc == null || args?.size !in 0..1 || args?.size == 1 && exc.arguments[0] !is StringLiteralExpr)
                        unsupported("$this (${this::class})", stmt)
                    else block.ReturnError(
                        if (args?.isEmpty() == true) exc.typeAsString
                        else (args?.get(0) as StringLiteralExpr).value
                    )
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            // AssertStmt
            fun handleAssertStmt(stmt: AssertStmt) {
                val check = stmt.check
                val guard = UnaryOperator.NOT.on(exp2Strudel.map(check))
                val statement = block.If(guard).block.ReturnError(
                    if (stmt.message.isPresent) exp2Strudel.map(stmt.message.get()).toString()
                    else "Assertion failed: $check"
                )
                stmt.comment.translateComment()?.let { statement.documentation = it }
                statement.bind(stmt)
            }

            when (stmt) {
                is ReturnStmt -> handleReturnStmt(stmt)
                is IfStmt -> handleIfStmt(stmt)
                is WhileStmt -> handleWhileStmt(stmt)
                is ForStmt -> handleForStmt(stmt)
                is ForEachStmt -> handleForEachStmt(stmt)
                is BreakStmt -> block.Break()
                is ContinueStmt -> block.Continue()
                is ExpressionStmt -> handleExpressionStmt(stmt)
                is ThrowStmt -> handleThrowStmt(stmt)
                is AssertStmt -> handleAssertStmt(stmt)
                else -> unsupported("statement $this (${this::class.qualifiedName})", stmt)
            }
        }
    }
}
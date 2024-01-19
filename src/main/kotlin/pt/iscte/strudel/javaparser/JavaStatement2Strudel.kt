package pt.iscte.strudel.javaparser

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.Type
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.impl.RecordFieldExpression
import pt.iscte.strudel.model.impl.VariableExpression

class JavaStatement2Strudel(
    val procedure: IProcedure,
    val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
    val types: MutableMap<String, IType>,
    val translator: Java2Strudel
) {

    fun translate(stmt: Statement, block: IBlock) {
        with(translator) {

            val exp2Strudel = JavaExpression2Strudel(procedure, block, procedures, types, translator)

            fun getType(type: Type): IType =
//            if (type.isArrayType)

//                if(types.containsKey(type.asString()))
//                    types[type.asString()]!!
//                else {
//                    val t = getType(type.elementType).array()
//                    types[type.elementType.asString() + "[]"] = t
//                    t
//                }
//            else
                types[type.asString()]!!

            fun findVariable(id: String): IVariableDeclaration<*>? =
                procedure.variables.find { it.id == id }
                    ?: procedure.parameters.find { it.id == THIS_PARAM }
                        ?.let { p ->
                            ((p.type as IReferenceType).target as IRecordType).getField(
                                id
                            )
                        }

            fun handleAssign(block: IBlock, a: AssignExpr): IStatement {
                if (a.target is NameExpr) {
                    val target = findVariable(a.target.toString())
                        ?: error(
                            "not found",
                            a
                        ) // UnboundVariableDeclaration(a.target.toString(), block)

                    val value = when (a.operator) {
                        AssignExpr.Operator.ASSIGN -> exp2Strudel.map(a.value)
                        else -> a.operator.map(a)
                            .on(exp2Strudel.map(a.target), exp2Strudel.map(a.value))
                    }

                    return if (target.isField)
                        block.FieldSet(
                            procedure.thisParameter.expression(),
                            target as IVariableDeclaration<IRecordType>,
                            value
                        )
                    else
                        block.Assign(target, value)

                } else if (a.target is ArrayAccessExpr) {
                    return when (a.operator) { // TODO compound assignment operators
                        AssignExpr.Operator.ASSIGN ->
                            block.ArraySet(
                                exp2Strudel.map((a.target as ArrayAccessExpr).name) as ITargetExpression,
                                exp2Strudel.map((a.target as ArrayAccessExpr).index),
                                exp2Strudel.map(a.value)
                            )

                        else -> unsupported("assign operator ${a.operator}", a)
                    }
                } else if (a.target is FieldAccessExpr) {
                    // val solve = jpFacade.solve((a.target as FieldAccessExpr).scope)
                    val typeId = if ((a.target as FieldAccessExpr).scope.isThisExpr)
                        procedure.namespace//(a.target as FieldAccessExpr).scope.asThisExpr().typeName.getOrNull?.id
                    else
                        JPFacade.solve((a.target as FieldAccessExpr).scope).correspondingDeclaration.type.asReferenceType().id

                    return when (a.operator) { // TODO compound assignment operators
                        AssignExpr.Operator.ASSIGN ->
                            block.FieldSet(
                                exp2Strudel.map((a.target as FieldAccessExpr).scope) as ITargetExpression,
                                types[typeId]?.asRecordType?.fields?.find { it.id == (a.target as FieldAccessExpr).nameAsString }
                                    ?: unsupported("field access", a),
                                exp2Strudel.map(a.value)
                            )

                        else -> unsupported("assign operator ${a.operator}", a)
                    }
                } else
                    unsupported("assignment", a)
            }


            fun handle(block: IBlock, s: Expression): IStatement =
                when (s) {
                    is AssignExpr -> handleAssign(block, s)

                    is UnaryExpr ->
                        // TODO array ++ --
                        if (s.operator == UnaryExpr.Operator.PREFIX_INCREMENT || s.operator == UnaryExpr.Operator.POSTFIX_INCREMENT) {
                            val varExp = exp2Strudel.map(s.expression)
                            if (varExp is VariableExpression)
                                block.Assign(varExp.variable, varExp + lit(1))
                            else if (varExp is RecordFieldExpression)
                                block.FieldSet(
                                    varExp.target,
                                    varExp.field,
                                    varExp + lit(1)
                                )
                            else
                                unsupported("${s.operator} on ${s.expression}", s)
                        } else if (s.operator == UnaryExpr.Operator.PREFIX_DECREMENT || s.operator == UnaryExpr.Operator.POSTFIX_DECREMENT) {
                            val varExp = exp2Strudel.map(s.expression)
                            if (varExp is VariableExpression)
                                block.Assign(varExp.variable, varExp - lit(1))
                            else if (varExp is RecordFieldExpression)
                                block.FieldSet(
                                    varExp.target,
                                    varExp.field,
                                    varExp - lit(1)
                                )
                            else
                                unsupported("${s.operator} on ${s.expression}", s)
                        } else
                            unsupported("unary expression statement", s)

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

            val s = when (stmt) {
                is ReturnStmt -> if (stmt.expression.isPresent)
                    block.Return(exp2Strudel.map(stmt.expression.get())).apply { setFlag() }
                else
                    block.ReturnVoid()

                is IfStmt -> block.If(exp2Strudel.map(stmt.condition)) {
                    translate(stmt.thenStmt, this)
                }.apply {
                    if (stmt.hasElseBranch()) {
                        createAlternativeBlock { translate(stmt.elseStmt.get(), it) }
                    }
                }

                is WhileStmt -> block.While(exp2Strudel.map(stmt.condition)) {
                    translate(stmt.body, this)
                }

                is ForStmt -> block.Block(FOR) {
                    stmt.initialization.forEach { i ->
                        if (i.isVariableDeclarationExpr) {
                            i.asVariableDeclarationExpr().variables.forEach { v ->
                                if (procedure!!.variables.any { it.id == v.nameAsString })
                                    unsupported("variables with same identifiers within the same procedure", i)
                                val varDec =
                                    addVariable(types.mapType(v.type)).apply {
                                        id = v.nameAsString
                                        setFlag(FOR)
                                        bind(i)
                                        bind(v.name, ID_LOC)
                                        bind(v.type, TYPE_LOC)
                                    }
                                if (v.initializer.isPresent)
                                    Assign(
                                        varDec,
                                        exp2Strudel.map(v.initializer.get())
                                    ).apply {
                                        setFlag(FOR)
                                        bind(v)
                                    }
                            }
                        } else
                            handle(this, i).bind(i)
                    }
                    val guard = if (stmt.compare.isPresent) exp2Strudel.map(stmt.compare.get()) else True
                    While(guard) {
                        translate(stmt.body, this)
                        stmt.update.forEach { u ->
                            handle(this, u).apply {
                                setFlag(FOR)
                                bind(u)
                            }
                        }
                    }.apply {
                        setFlag(FOR)
                    }
                }

                is ForEachStmt -> block.Block(EFOR) {
                    if (procedure!!.variables.any { it.id == stmt.variable.variables[0].nameAsString })
                        unsupported("variables with same identifiers within the same procedure", stmt.variable)

                    val itVar =
                        addVariable(types.mapType(stmt.variable.elementType)).apply {
                            val v = stmt.variable.variables[0]
                            id = v.nameAsString
                            setFlag(EFOR)
                            bind(stmt.variable)
                            bind(v.name, ID_LOC)
                            bind(v.type, TYPE_LOC)
                        }
                    val indexVar = addVariable(INT).apply {
                        id = IT + this@Block.depth
                        setFlag(EFOR)
                        bind(stmt.variable)
                    }
                    Assign(indexVar, lit(0)).apply {
                        setFlag(EFOR)
                    }
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

                is BreakStmt -> block.Break()

                is ContinueStmt -> block.Continue()

                is ExpressionStmt ->
                    if (stmt.expression is VariableDeclarationExpr) {
                        if ((stmt.expression as VariableDeclarationExpr).variables.size > 1)
                            unsupported(
                                "multiple variable declarations",
                                stmt.expression
                            )

                        val dec = (stmt.expression as VariableDeclarationExpr).variables[0]

                        val type = getType(dec.type)
                        run {
                            if (procedure.variables.any { it.id == dec.nameAsString })
                                unsupported("variables with same identifiers within the same procedure", stmt.expression)

                            val varDec = block.Var(type, dec.nameAsString)
                            if (dec.initializer.isPresent) {
                                block.Assign(
                                    varDec,
                                    exp2Strudel.map(dec.initializer.get())
                                ).bind(stmt)
                            }
                            varDec.bind(dec.type, TYPE_LOC)
                                .bind(dec.name, ID_LOC)
                        }
                    } else
                        handle(block, stmt.expression)

                is ThrowStmt -> {
                    // throw statement only supported if expression is object creation with single String argument
                    val exc = stmt.expression as? ObjectCreationExpr

                    if (exc == null ||
                        exc.arguments.size !in 0..1 ||
                        exc.arguments.size == 1 && exc.arguments[0] !is StringLiteralExpr
                    )
                        unsupported("$this (${this::class})", stmt)
                    else {
                        val msg = if (exc.arguments.isEmpty())
                            exc.typeAsString
                        else
                            (exc.arguments[0] as StringLiteralExpr).value
                        block.ReturnError(msg)
                    }
                }

                else -> unsupported("$this (${this::class})", stmt)
            }
            stmt.comment.translateComment()?.let { s.documentation = it }
            s.bind(stmt)
        }
    }
}
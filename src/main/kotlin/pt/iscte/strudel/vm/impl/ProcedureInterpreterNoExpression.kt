package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.get
import pt.iscte.strudel.model.impl.Literal
import pt.iscte.strudel.model.impl.PredefinedArrayAllocation
import pt.iscte.strudel.model.util.*
import pt.iscte.strudel.vm.*
import java.lang.RuntimeException


class ProcedureInterpreterNoExpression(
    val vm: IVirtualMachine,
    val procedure: IProcedure,
    vararg val arguments: IValue
) {
    var returnValue: IValue? = null

    val instructionPointer: IProgramElement?
        get() = if (blockStack.isEmpty()) null
        else
            blockStack.top.current

    val currentExpression: IExpression?
        get() =
            null

    fun <T> MutableList<T>.push(e: T) = add(e)
    fun <T> MutableList<T>.pop(): T {
        val last = last()
        removeAt(size - 1)
        return last
    }

    val <T> MutableList<T>.top: T get() = last()

    val blockStack = mutableListOf<BlockExec>()
    val loopCount =
        procedure.findAll(ILoop::class).associateWith { 0 }.toMutableMap()

    fun isOver() = blockStack.isEmpty()

    fun init() {
        vm.callStack.newFrame(procedure, arguments.toList())
        blockStack.add(BlockExec(procedure.block, false))
    }

    fun run(): IValue? {
        init()
        val args = arguments.toList()
        vm.listeners.forEach {
            it.procedureCall(
                procedure,
                args,
                vm.callStack.previousFrame?.procedure
            )
        }
        while (!isOver())
            step()
        vm.listeners.forEach { it.procedureEnd(procedure, args, returnValue) }
        vm.callStack.terminateTopFrame()
        return returnValue
    }


    inner class BlockExec(
        val block: IBlock,
        val isLoop: Boolean,
        var index: Int = 0
    ) {
        val isOver: Boolean get() = index == block.size

        val current: IProgramElement? get() = if (isOver) null else block.children[index]

        fun execute() {
            //check(!isOver)
            when (val next = block.children[index]) {
                is IStatement -> {
                    if (execute(next))
                        index++
                }

                is IBlock -> {
                    blockStack.push(BlockExec(next, false))
                    index++
                }

                is ILoop -> {
                    val guardEval = evaluate(next.guard)
                    vm.listeners.forEach { l ->
                        l.expressionEvaluation(
                            next.expression,
                            next,
                            guardEval,
                            next.expression.materialize()
                        )
                    }
                    if (guardEval.isTrue) {
                        blockStack.push(BlockExec(next.block, true))
                        if (loopCount[next] == vm.loopIterationMaximum)
                            throw RuntimeError(
                                RuntimeErrorType.LOOP_MAX,
                                next,
                                "Loop reached the maximum number of iterations (${vm.loopIterationMaximum})."
                            )
                        loopCount[next] = (loopCount[next] ?: 0) + 1
                        vm.listeners.forEach {
                            it.loopIteration(next)
                        }
                    } else {
                        index++
                        loopCount[next] = 0
                        vm.listeners.forEach {
                            it.loopEnd(next)
                        }
                    }
                }

                is ISelection -> {
                    val guardEval = evaluate(next.guard)
                    vm.listeners.forEach { l ->
                        l.expressionEvaluation(
                            next.expression,
                            next,
                            guardEval,
                            next.expression.materialize()
                        )
                    }
                    if (guardEval.isTrue) {
                        blockStack.push(BlockExec(next.block, false))
                    } else if (next.hasAlternativeBlock()) {
                        blockStack.push(
                            BlockExec(
                                next.alternativeBlock!!,
                                false
                            )
                        )
                    }
                    index++
                }
                else -> index++
            }
        }
    }


    fun step() {
        while (blockStack.isNotEmpty() && blockStack.top.isOver)
            blockStack.pop()

        if (blockStack.isNotEmpty())
            blockStack.top.execute()
    }


    private fun IExpression.materialize(): IExpression =
        try {
            when (this) {
                is IVariableExpression -> if (type.isValueType)
                    Literal(
                        type,
                        vm.callStack.topFrame.variables[variable].toString()
                    )
                else
                    this

                is IBinaryExpression -> operator.on(
                    leftOperand.materialize(),
                    rightOperand.materialize()
                )

//                is IUnaryExpression -> "${e.operator}${materialize(e.operand)}"
//                is IProcedureCallExpression -> "${e.procedure.id}(${e.arguments.joinToString(", ") { materialize(it) }})"
                is IArrayAccess -> if (target is IVariableExpression)
                    (target as IVariableExpression).variable[index.materialize()]
                else
                    this
//
////                is IArrayAccess -> if (e.target is IVariableExpression)
////                    (vm.topFrame.variables[(e.target as IVariableExpression).variable] as IReference<IArray>).target.getElement((materialize(e.index) as IValue).toInt()).toString()
////                else
////                    this.toString()
                is IArrayLength -> if (target is IVariableExpression)
                    Literal(
                        INT,
                        (vm.topFrame.variables[(target as IVariableExpression).variable] as IReference<IArray>).target.length.toString()
                    )
                else
                    this
//                //  is IRecordFieldExpression -> "${materialize(e.target)}.${e.field}"
//                is IConditionalExpression -> "${materialize(e.condition)} ? ${materialize(e.trueCase)} : ${materialize(e.falseCase)}"
//                is IArrayAllocation -> "new ${e.componentType}[${e.dimensions.joinToString(", ") { materialize(it) }}]"
                else -> this
            }
        } catch (e: Exception) {
            throw RuntimeException("Invalid expression for current context: $e")
        }


    private fun execute(s: IStatement): Boolean {
        when (s) {
            is IVariableAssignment -> {
                val value = evaluate(s.expression)
                vm.listeners.forEach { l ->
                    l.expressionEvaluation(
                        s.expression,
                        s,
                        value,
                        s.expression.materialize()
                    )
                }
                vm.callStack.topFrame[s.target] = value
                vm.listeners.forEach { l ->
                    l.statement(s)
                    l.variableAssignment(s, value)
                }
                return true
            }

            is IArrayElementAssignment -> {
                val array = evaluate(s.arrayAccess.target)
                val index = evaluate(s.arrayAccess.index)
                val value = evaluate(s.expression)

                vm.listeners.forEach { l ->
                    l.expressionEvaluation(
                        s.expression,
                        s,
                        value,
                        s.expression.materialize()
                    )
                }
                val i = index.toInt()
                if (i < 0 || i >= ((array as IReference<IArray>).target).length)
                    throw ArrayIndexError(
                        s.arrayAccess,
                        i,
                        s.arrayAccess.index,
                        (array as IReference<IArray>).target
                    )

                array.target.setElement(i, value)
                vm.listeners.forEach { l ->
                    l.statement(s)
                    l.arrayElementAssignment(s, array, i, value)
                }
                return true
            }

            is IRecordFieldAssignment -> {
                val target = evaluate(s.target)
                val value = evaluate(s.expression)
                vm.listeners.forEach { l ->
                    l.expressionEvaluation(
                        s.expression,
                        s,
                        value,
                        s.expression.materialize()
                    )
                }
                val recordRef = target as IReference<IRecord>
                recordRef.target.setField(s.field, value)
                vm.listeners.forEach { l ->
                    l.statement(s)
                    l.fieldAssignment(s, recordRef, value)
                }
                return true
            }

            is IReturn ->
                if (s.isError) {
                    throw ExceptionError(s, s.errorMessage.toString())
                } else if (s.expression != null) {
                    val value = evaluate(s.expression!!)
                    vm.listeners.forEach { l ->
                        l.expressionEvaluation(
                            s.expression!!,
                            s,
                            value,
                            s.expression!!.materialize()
                        )
                    }
                    returnValue = value
                    vm.callStack.topFrame.returnValue = value
                    blockStack.clear()
                    vm.listeners.forEach { l ->
                        l.statement(s)
                        l.returnCall(s, returnValue)
                    }
                    return true
                } else {
                    vm.callStack.topFrame.returnValue = NULL
                    blockStack.clear()
                    vm.listeners.forEach { l ->
                        l.statement(s)
                        l.returnCall(s, returnValue)
                    }
                    return true
                }

            is IProcedureCall -> {
                vm.listeners.forEach { l ->
                    l.statement(s)
                }
                handleProcedureCall(
                    s.procedure,
                    s.arguments.map { evaluate(it) })
                return true
            }

            is IBreak -> {
                while (!blockStack.top.isLoop)
                    blockStack.pop()

                blockStack.pop()
                blockStack.top.index++
                vm.listeners.forEach { l ->
                    l.statement(s)
                }
            }

            is IContinue -> {
                while (!blockStack.top.isLoop)
                    blockStack.pop()
                blockStack.pop()
                vm.listeners.forEach { l ->
                    l.statement(s)
                }
            }

            else -> {
                throw RuntimeError(
                    RuntimeErrorType.UNSUPPORTED,
                    s,
                    "not handled"
                )
            }
        }
        return false
    }

    private fun evaluate(exp: IExpression): IValue =
        when (exp) {
            is ILiteral -> when (exp.type) {
                INT -> Value(INT, exp.stringValue.toInt())
                DOUBLE -> Value(DOUBLE, exp.stringValue.toDouble())
                BOOLEAN -> Value(BOOLEAN, exp.stringValue.toBoolean())
                CHAR -> Value(CHAR, exp.stringValue[0])
                else -> NULL
            }

            is IVariableExpression -> {
                val v = vm.callStack.topFrame.variables[exp.variable]
                if (v == null)
                    throw RuntimeError(
                        RuntimeErrorType.NONINIT_VARIABLE,
                        exp,
                        "variable not initialized"
                    )
                else
                    v
            }

            // TODO only works for 1 dim
            is PredefinedArrayAllocation -> {
                vm.allocateArrayOfValues(
                    exp.componentType,
                    exp.elements.map { evaluate(it) })
            }

            is IArrayAllocation -> {
                val dims = exp.dimensions.map { evaluate(it) }
                dims.forEachIndexed { i, d ->
                    if (d.toInt() < 0)
                        throw NegativeArraySizeError(
                            exp,
                            d.toInt(),
                            exp.dimensions[i]
                        )
                }

                var baseType = exp.componentType
                repeat(exp.dimensions.size - 1) {
                    baseType = baseType.array()
                }

                val dim = dims[0].toInt()
                val arrayRef = vm.allocateArray(baseType, dim)

                // TODO other dims? only works for 1 and 2
                for (d in dims.drop(1)) {
                    for (i in 0 until dim) {
                        arrayRef.target.setElement(
                            i,
                            vm.allocateArray(exp.componentType, d.toInt())
                        )
                    }
                }
                arrayRef
            }

            is IArrayAccess -> {
                val index = evaluate(exp.index)
                val array = evaluate(exp.target)
                if (array.isNull)
                    throw RuntimeError(
                        RuntimeErrorType.NULL_POINTER,
                        exp.target,
                        "reference to array is null"
                    )

                val i = index.toInt()
                if (i < 0 || i >= ((array as IReference<IArray>).target).length)
                    throw ArrayIndexError(
                        exp,
                        i,
                        exp.index,
                        (array as IReference<IArray>).target
                    )

                array.target.getElement(i)
            }

            is IArrayLength ->  {
                val it = evaluate(exp.target)
                if (it.isNull)
                    throw RuntimeError(
                        RuntimeErrorType.NULL_POINTER,
                        exp.target,
                        "reference to array is null"
                    )

                vm.getValue(((it as IReference<IArray>).target).length)
            }

            is IRecordFieldExpression -> {
                val it = evaluate(exp.target)
                if (it.isNull)
                    throw RuntimeError(
                        RuntimeErrorType.NULL_POINTER,
                        exp.target,
                        "reference to object is null"
                    )
                (it as IReference<IRecord>).target.getField(exp.field)
            }

            is IBinaryExpression -> binoperation(
                exp,
                evaluate(exp.leftOperand),
                evaluate(exp.rightOperand)
            )

            is IUnaryExpression -> unopearation(
                exp.operator,
                exp.type,
                evaluate(exp.operand)
            )

            is IProcedureCallExpression -> handleProcedureCall(
                exp.procedure,
                exp.arguments.map { evaluate(it) })

            is IRecordAllocation -> {
                vm.allocateRecord(exp.recordType)
            }

            is IConditionalExpression ->
                if (evaluate(exp.condition).isTrue)
                    evaluate(exp.trueCase)
                else
                    evaluate(exp.falseCase)

            else -> throw UnsupportedOperationException(exp.toString())
        }


    // TODO what if first element is null in instance procedure?
    private fun handleProcedureCall(
        procDec: IProcedureDeclaration,
        args: List<IValue>
    ): IValue {
        if (procDec.isForeign) {
            return (procDec as ForeignProcedure).run(vm, args) ?: NULL
        } else {
            val proc: IProcedureDeclaration =
                if (procDec is IPolymorphicProcedure)
                    procDec.module?.procedures?.find { p ->
                        p.id == procDec.id && p.namespace == args[0].type.id
                    }
                        ?: throw UnsupportedOperationException("Could not find procedure ${procDec.id} within namespace ${args[0].type.id}")
                else
                    procDec as IProcedure

            if (proc.isForeign) {
                val run = (proc as ForeignProcedure).run(vm, args)
                return run!!
            } else {
                val call = ProcedureInterpreterNoExpression(
                    vm,
                    proc as IProcedure,
                    *args.toTypedArray()
                )
                call.run()
                if (!proc.returnType.isVoid)
                    return call.returnValue!!
                else
                    return NULL
            }
        }
        throw UnsupportedOperationException("Could not find procedure ${procDec.id} within namespace ${args[0].type.id}")
    }

    private fun unopearation(
        operator: IUnaryOperator,
        type: IType,
        value: IValue
    ): IValue =
        when (operator) {
            UnaryOperator.NOT -> Value(BOOLEAN, !value.toBoolean())
            UnaryOperator.PLUS ->
                if (type == INT) Value(INT, +value.toInt())
                else Value(DOUBLE, +value.toDouble())

            UnaryOperator.MINUS ->
                if (type == INT) Value(INT, -value.toInt())
                else Value(DOUBLE, -value.toDouble())

            UnaryOperator.TRUNCATE -> Value(INT, value.toDouble().toInt())
            else -> throw UnsupportedOperationException(operator.toString())
        }

    val div0message = "Cannot divide by zero"
    private fun binoperation(
        exp: IBinaryExpression,
        left: IValue,
        right: IValue
    ): IValue {
        val operator = exp.operator
        val type = exp.type
        if (operator is ArithmeticOperator) {
            if (operator == ArithmeticOperator.IDIV)
                return if (right.toInt() == 0)
                    throw RuntimeError(
                        RuntimeErrorType.DIVBYZERO,
                        exp.rightOperand,
                        div0message
                    )
                else Value(INT, left.toInt() / right.toInt())
            if (operator == ArithmeticOperator.MOD)
                return if (right.toInt() == 0)
                    throw RuntimeError(
                        RuntimeErrorType.DIVBYZERO,
                        exp.rightOperand,
                        div0message
                    )
                else Value(INT, left.toInt() % right.toInt())


            val l: Double = left.toDouble()
            val r: Double = right.toDouble()
            var res = when (operator) {
                ArithmeticOperator.ADD -> l + r
                ArithmeticOperator.SUB -> l - r
                ArithmeticOperator.MUL -> l * r
                ArithmeticOperator.DIV -> if (r == 0.0)
                    throw RuntimeError(
                        RuntimeErrorType.DIVBYZERO,
                        exp.rightOperand,
                        div0message
                    )
                else
                    l / r

                else -> throw UnsupportedOperationException(operator.toString())
            }

            return if (type == INT) Value(type, res.toInt()) else Value(
                type,
                res
            )
        } else if (operator is RelationalOperator) {
            if (operator == RelationalOperator.EQUAL) return Value(
                BOOLEAN,
                left.value == right.value
            )
            if (operator == RelationalOperator.DIFFERENT) return Value(
                BOOLEAN,
                left.value != right.value
            )

            val l: Double = left.toDouble()
            val r: Double = right.toDouble()
            var res = when (operator) {
                RelationalOperator.SMALLER -> l < r
                RelationalOperator.SMALLER_EQUAL -> l <= r
                RelationalOperator.GREATER -> l > r
                RelationalOperator.GREATER_EQUAL -> l >= r

                else -> throw UnsupportedOperationException(operator.toString())
            }
            return Value(type, res)
        } else if (operator is LogicalOperator) {
            var res = when (operator) {
                LogicalOperator.AND -> left.toBoolean() && right.toBoolean()
                LogicalOperator.OR -> left.toBoolean() || right.toBoolean()
                LogicalOperator.XOR -> left.toBoolean() xor right.toBoolean()
                else -> throw UnsupportedOperationException(operator.toString())
            }
            return Value(type, res)
        }
        throw UnsupportedOperationException(operator.toString())
    }
}


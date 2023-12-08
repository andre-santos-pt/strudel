package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.PredefinedArrayAllocation
import pt.iscte.strudel.model.util.*
import pt.iscte.strudel.vm.*



class ProcedureInterpreter(
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
        get() = if (expStack.isEmpty())
            null
        else
            expStack.top


    fun <T> MutableList<T>.push(e: T) = add(e)
    fun <T> MutableList<T>.pop(): T {
        val last = last()
        removeAt(size - 1)
        return last
    }

    val <T> MutableList<T>.top: T get() = last()

    val expStack = mutableListOf<IExpression>()
    val valStack = mutableListOf<IValue>()
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
            check(!isOver)
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
                    if (valStack.isEmpty())
                        evaluateStep(next.guard)
                    else {
                        if (valStack.pop().isTrue) {
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
                        }
                    }
                }
                is ISelection -> {
                    if (valStack.isEmpty())
                        evaluateStep(next.guard)
                    else {
                        if (valStack.pop().isTrue) {
                            blockStack.push(BlockExec(next.block, false))
                        }
                        else if (next.hasAlternativeBlock()) {
                            blockStack.push(
                                BlockExec(
                                    next.alternativeBlock!!,
                                    false
                                )
                            )
                        }
                        index++
                    }
                }
                else -> index++
            }
        }
    }


    fun stepStatement() {
        while(expStack.isNotEmpty())
            step()

        step()
    }

    fun step() {
//        while (expStack.isNotEmpty() && expStack.top is ILiteral)
//            evaluate(expStack.pop())

        if (expStack.isNotEmpty()) {
            evaluate(expStack.pop())
        } else {
            while (blockStack.isNotEmpty() && blockStack.top.isOver)
                blockStack.pop()

            if (blockStack.isNotEmpty())
                blockStack.top.execute()
        }

    }

    private fun eval(e: IExpression): IValue? =
        if (valStack.isEmpty()) {
            evaluateStep(e)
            null
        } else
            valStack.pop()

    private fun eval(expressions: List<IExpression>): List<IValue>? =
        if (valStack.size < expressions.size) {
            expressions.forEach {
                evaluateStep(it)
            }
            null
        } else {
            expressions.map { valStack.pop() }
        }


    private fun eval(vararg expressions: IExpression): List<IValue>? =
        eval(expressions.toList())

    private fun evaluateStep(e: IExpression) {
        expStack.push(e)
        if (e is ICompositeExpression)
            for (i in e.parts.lastIndex downTo 0)
                evaluateStep(e.parts[i])

    }

    private fun execute(s: IStatement): Boolean {
        when (s) {
            is IVariableAssignment -> eval(s.expression)?.let {
                vm.callStack.topFrame[s.target] = it
                vm.listeners.forEach { l -> l.variableAssignment(s, it) }
                return true
            }

            is IArrayElementAssignment -> eval(
                s.arrayAccess.target,
                s.arrayAccess.index,
                s.expression
            )?.let {
                val (array, index, value) = it
                val i = index.toInt()
                if (i < 0 || i >= ((array as IReference<IArray>).target).length)
                    throw ArrayIndexError(s.arrayAccess, i, s.arrayAccess.index)

                (array.target as IArray).setElement(i, value)
                vm.listeners.forEach { l ->
                    l.arrayElementAssignment(s, array, i, value)
                }
                return true
            }

            is IRecordFieldAssignment -> eval(s.target, s.expression)?.let {
                val (target, value) = it
                ((target as IReference<IRecord>).target).setField(
                    s.field,
                    value
                )
                vm.listeners.forEach { l -> l.fieldAssignment(s, value) }
                return true
            }

            is IReturn ->
                if (s.expression != null) {
                    val e = eval(s.expression!!)
                    e?.let {
                        returnValue = it
                        vm.callStack.topFrame.returnValue = it
                        blockStack.clear()
                        vm.listeners.forEach { l ->
                            l.returnCall(
                                s,
                                returnValue
                            )
                        }
                    }
                    return e != null
                } else {
                    vm.callStack.topFrame.returnValue = NULL
                    blockStack.clear()
                    vm.listeners.forEach { it.returnCall(s, returnValue) }
                    return true
                }

            is IProcedureCall -> eval(
                *s.arguments.reversed().toTypedArray()
            )?.reversed()?.let {
                if (s.procedure.isForeign) {
                    (s.procedure as ForeignProcedure).run(vm, it)
                } else {
                    val call = ProcedureInterpreter(
                        vm,
                        s.procedure as IProcedure,
                        *it.toTypedArray()
                    );
                    call.run()
                    // TODO check error
                }
                return true
            }

            is IBreak -> {
                while (!blockStack.top.isLoop)
                    blockStack.pop()

                blockStack.pop()
                blockStack.top.index++
            }
            is IContinue -> {
                while (!blockStack.top.isLoop)
                    blockStack.pop()
                blockStack.pop()
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

    private fun evaluate(exp: IExpression) {
        when (exp) {
            is ILiteral -> {
                val lit = when (exp.type) {
                    INT -> Value(INT, exp.stringValue.toInt())
                    DOUBLE -> Value(DOUBLE, exp.stringValue.toDouble())
                    BOOLEAN -> Value(BOOLEAN, exp.stringValue.toBoolean())
                    CHAR -> Value(CHAR, exp.stringValue[0])
                    else -> NULL
                }
                valStack.push(lit)
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
                    valStack.push(v)
            }

            // TODO only works for 1 dim
            is PredefinedArrayAllocation -> eval(exp.elements)?.let {
                val arrayRef =
                    vm.allocateArray(exp.componentType, exp.elements.size)
                it.reversed().forEachIndexed { index, e ->
                    arrayRef.target.setElement(index, e)
                }
                valStack.push(arrayRef)
            }

            is IArrayAllocation -> eval(exp.dimensions)?.let { dims ->
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

                var dim = dims[0].toInt()
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

                valStack.push(arrayRef)
            }

            is IArrayAccess -> eval(exp.target, exp.index)?.let {
                val (index, array) = it
                val i = index.toInt()
                if (i < 0 || i >= ((array as IReference<IArray>).target).length)
                    throw ArrayIndexError(exp, i, exp.index)

                valStack.push((array.target as IArray).getElement(i))
            }

            is IArrayLength -> eval(exp.target)?.let {
                valStack.push(vm.getValue(((it as IReference<IArray>).target).length))
            }

            is IRecordFieldExpression -> eval(exp.target)?.let {
                valStack.push(((it as IReference<IRecord>).target).getField(exp.field))
            }

            is IBinaryExpression -> {
                if (valStack.size < 2) {
                    evaluateStep(exp.rightOperand)
                    evaluateStep(exp.leftOperand)
                }
                val right = valStack.pop()
                val left = valStack.pop()
                valStack.push(binoperation(exp, left, right))
            }

            is IUnaryExpression -> eval(exp.operand)?.let {
                valStack.push(unopearation(exp.operator, exp.type, it))
            }

            is IProcedureCallExpression -> {
                eval(*exp.arguments.reversed().toTypedArray())?.reversed()
                    ?.let {
                        // TODO check error
                        if (exp.procedure.isForeign) {
                            val ret =
                                (exp.procedure as ForeignProcedure).run(vm, it)
                            ret?.let {
                                valStack.push(ret)
                            }
                        } else {
                            val call = ProcedureInterpreter(
                                vm,
                                exp.procedure as IProcedure,
                                *it.toTypedArray()
                            )
                            call.run()
                            valStack.push(call.returnValue!!) // not void
                        }
                    }
            }

            is IRecordAllocation -> {
                valStack.push(vm.allocateRecord(exp.recordType))
            }
            else -> throw UnsupportedOperationException(exp.toString())
        }
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
                return if(right.toInt() == 0)
                    throw RuntimeError(RuntimeErrorType.DIVBYZERO, exp.rightOperand, div0message)
                else Value(INT, left.toInt() / right.toInt())
            if (operator == ArithmeticOperator.MOD)
                return if(right.toInt() == 0)
                    throw RuntimeError(RuntimeErrorType.DIVBYZERO, exp.rightOperand, div0message)
                else Value(INT, left.toInt() % right.toInt())


            val l: Double = left.toDouble()
            val r: Double = right.toDouble()
            var res = when (operator) {
                ArithmeticOperator.ADD -> l + r
                ArithmeticOperator.SUB -> l - r
                ArithmeticOperator.MUL -> l * r
                ArithmeticOperator.DIV -> if(r == 0.0)
                    throw RuntimeError(RuntimeErrorType.DIVBYZERO, exp.rightOperand, div0message)
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

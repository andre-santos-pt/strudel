package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.get
import pt.iscte.strudel.model.impl.ArrayLength
import pt.iscte.strudel.model.impl.Conditional
import pt.iscte.strudel.model.impl.Literal
import pt.iscte.strudel.model.impl.PredefinedArrayAllocation
import pt.iscte.strudel.model.util.*
import pt.iscte.strudel.vm.*
import java.lang.reflect.InvocationTargetException

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
        get() = null // TODO?

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
                        if (vm.loopIterationMaximum != null && loopCount[next] == vm.loopIterationMaximum)
                            throw LoopIterationLimitError(
                                next,
                                vm.loopIterationMaximum!!
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

                is IUnaryExpression -> operator.on(operand.materialize())

                is IConditionalExpression -> Conditional(
                    condition.materialize(),
                    trueCase.materialize(),
                    falseCase.materialize()
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
                else if (target is IArrayAccess) {
                    val t = target.materialize()
                    if (t is ITargetExpression)
                        ArrayLength(t)
                    else
                        this
                }
                else
                    this

                is IProcedureCallExpression -> this.procedure.expression(
                    this.arguments.map { it.materialize() }
                )
//                //  is IRecordFieldExpression -> "${materialize(e.target)}.${e.field}"
//                is IConditionalExpression -> "${materialize(e.condition)} ? ${materialize(e.trueCase)} : ${materialize(e.falseCase)}"
//                is IArrayAllocation -> "new ${e.componentType}[${e.dimensions.joinToString(", ") { materialize(it) }}]"
                else -> this
            }
        } catch (e: Exception) {
            throw RuntimeException("Invalid expression for current context: $e")
        }

    private fun execute(s: IStatement): Boolean {
        fun unsupported(msg: String): Nothing =
            throw RuntimeError(RuntimeErrorType.UNSUPPORTED, s, msg)

        // Implicit upcasting
        fun IType.upcast(value: IValue): IValue = when (this) {
            CHAR -> when (value.type) {
                CHAR -> value
                INT -> vm.getValue(value.toInt().toChar())
                DOUBLE -> vm.getValue(value.toDouble().toInt().toChar())
                else -> unsupported("implicit cast of ${value.type.id} to $id")
            }

            INT -> when (value.type) {
                CHAR -> vm.getValue(value.toChar().code)
                INT -> value
                else -> unsupported("implicit cast of ${value.type.id} to $id")
            }

            DOUBLE -> when (value.type) {
                CHAR -> vm.getValue(value.toChar().code.toDouble())
                INT -> vm.getValue(value.toDouble())
                DOUBLE -> value
                else -> unsupported("implicit cast of ${value.type.id} to $id")
            }

            else -> value
        }

        when (s) {
            is IVariableAssignment -> {
                val value = s.target.type.upcast(evaluate(s.expression))
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
                val componentType = when (val t = s.arrayAccess.target.type) {
                    is IArrayType -> t.componentType
                    is IReferenceType -> t.target.asArrayType.componentType
                    else -> t
                }
                val value = componentType.upcast(evaluate(s.expression))

                vm.listeners.forEach { l ->
                    l.expressionEvaluation(
                        s.expression,
                        s,
                        value,
                        s.expression.materialize()
                    )
                }

                if (array.isNull)
                    throw NullReferenceError(s.arrayAccess.target)

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
                val value = s.target.type.upcast(evaluate(s.expression))
                vm.listeners.forEach { l ->
                    l.expressionEvaluation(
                        s.expression,
                        s,
                        value,
                        s.expression.materialize()
                    )
                }

                if (target.isNull)
                    throw NullReferenceError(s.target)

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
                    val error =
                        if (s.expression != null) evaluate(s.expression!!) else NULL
                    val errorMessage =
                        if (error.isNull) s.error?.toString() else error.toString()
                    throw RuntimeError(
                        RuntimeErrorType.EXCEPTION,
                        s,
                        errorMessage
                    )
                } else if (s.expression != null) {
                    val value =
                        if (s.isVoid) evaluate(s.expression!!)
                        else s.ownerProcedure.returnType.upcast(evaluate(s.expression!!))
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
                handleProcedureCall(s)
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

    fun evaluate(exp: IExpression): IValue =
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
                v ?: throw UninitializedVariableError(exp)
            }

            is PredefinedArrayAllocation -> {
                vm.allocateArrayOf(
                    exp.componentType,
                    *exp.elements.map { evaluate(it) }.toTypedArray()
                )
            }

            is IArrayAllocation -> {
                val dims = exp.dimensions.map { evaluate(it) }
                dims.forEachIndexed { i, d ->
                    if (d.toInt() < 0)
                        throw NegativeArraySizeError(
                            exp.dimensions[i],
                            d.toInt()
                        )
                }

                fun recAllocate(type: IType, dimIndex: Int): IReference<IArray> {
                    val dim = dims[dimIndex].toInt()
                    val compType = if(type is IArrayType) type.componentType else type
                    val arrayRef = vm.allocateArray(compType, dim)
                    if (dimIndex < dims.size - 1) {
                        for (i in 0 until dim) {
                            arrayRef.target.setElement(i, recAllocate(compType, dimIndex + 1))
                        }
                    }
                    return arrayRef
                }

                if (dims.isEmpty())
                    throw RuntimeError(RuntimeErrorType.UNSUPPORTED, exp, "zero-dimension arrays not supported")
                else
                    recAllocate(procedure.module?.getArrayType(exp.componentType, dims.size) ?: exp.componentType.array(dims.size), 0)
            }

            is IArrayAccess -> {
                val index = evaluate(exp.index)
                val array = evaluate(exp.target)
                if (array.isNull)
                    throw NullReferenceError(exp.target)

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

            is IArrayLength -> {
                val it = evaluate(exp.target)
                if (it.isNull)
                    throw NullReferenceError(exp.target)

                vm.getValue(((it as IReference<IArray>).target).length)
            }

            is IRecordFieldExpression -> {
                val it = evaluate(exp.target)
                if (it.isNull)
                    throw NullReferenceError(exp.target)
                (it as IReference<IRecord>).target.getField(exp.field)
            }

            is IBinaryExpression -> binoperation(
                exp,
                evaluate(exp.leftOperand),
                evaluate(exp.rightOperand)
            )

            is IUnaryExpression -> {
                val operand = evaluate(exp.operand)
                unopearation(exp.operator, operand.type, operand)
            }

            is IProcedureCallExpression -> handleProcedureCall(exp)

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
        call: IProcedureCall
    ): IValue {
        val args = call.arguments.map { evaluate(it) }

        if (call.procedure.isForeign) {
            vm.listeners.forEach {
                it.procedureCall(
                    call.procedure,
                    args,
                    vm.callStack.topFrame.procedure
                )
            }
            val ret = try {
                (call.procedure as ForeignProcedure).run(vm, args) ?: NULL
            } catch (e: InvocationTargetException) {
                throw RuntimeError(
                    RuntimeErrorType.FOREIGN_PROCEDURE,
                    call,
                    e.message ?: e.targetException.message
                )
            }
            vm.listeners.forEach {
                it.procedureEnd(
                    call.procedure,
                    args,
                    ret
                )
            }
            return ret
        } else {
            val proc: IProcedureDeclaration =
                if (call.procedure is IPolymorphicProcedure)
                    call.procedure.module?.procedures?.find { p ->
                        p.id == call.procedure.id && p.namespace == args[0].type.id
                    }
                        ?: throw UnsupportedOperationException("Could not find procedure ${call.id} within namespace ${args[0].type.id}")
                else
                    call.procedure as IProcedure

            if (proc.isForeign) {
                vm.listeners.forEach {
                    it.procedureCall(
                        proc,
                        args,
                        vm.callStack.topFrame.procedure
                    )
                }
                val run = (proc as ForeignProcedure).run(vm, args)
                vm.listeners.forEach {
                    it.procedureEnd(
                        proc,
                        args,
                        run
                    )
                }
                return run ?: NULL
            } else {
                val callInterpreter = ProcedureInterpreterNoExpression(
                    vm,
                    proc as IProcedure,
                    *args.toTypedArray()
                )
                callInterpreter.run()
                if (!proc.returnType.isVoid)
                    return callInterpreter.returnValue!!
                else
                    return NULL
            }
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

            UnaryOperator.CAST_TO_INT ->
                if (type.isNumber) Value(INT, value.toDouble().toInt())
                else Value(INT, value.toChar().code)

            UnaryOperator.CAST_TO_DOUBLE ->
                if (type.isNumber) Value(DOUBLE, value.toDouble())
                else Value(DOUBLE, value.toChar().code.toDouble())

            UnaryOperator.CAST_TO_CHAR ->
                if (type.isNumber) Value(
                    CHAR,
                    value.toDouble().toInt().toChar()
                )
                else Value(CHAR, value.toChar())

            else -> throw UnsupportedOperationException(operator.toString())
        }

    private fun binoperation(
        exp: IBinaryExpression,
        left: IValue,
        right: IValue
    ): IValue {
        fun unsupported(): Nothing =
            throw RuntimeError(
                RuntimeErrorType.UNSUPPORTED,
                exp,
                "operator ${exp.operator} between ${left.type.id} and ${right.type.id}"
            )

        val operator = exp.operator
        val type = exp.type
        return when (operator) {
            is ArithmeticOperator -> when (operator) {
                ArithmeticOperator.BITWISE_XOR -> Value(
                    type,
                    left.toInt() xor right.toInt()
                )

                ArithmeticOperator.BITWISE_AND -> Value(
                    type,
                    left.toInt() and right.toInt()
                )

                ArithmeticOperator.BITWISE_OR -> Value(
                    type,
                    left.toInt() or right.toInt()
                )

                ArithmeticOperator.LEFT_SHIFT -> Value(
                    type,
                    left.toInt() shl right.toInt()
                )

                ArithmeticOperator.SIGNED_RIGHT_SHIFT -> Value(
                    type,
                    left.toInt() shr right.toInt()
                )

                ArithmeticOperator.UNSIGNED_RIGHT_SHIFT -> Value(
                    type,
                    left.toInt() ushr right.toInt()
                )

                ArithmeticOperator.IDIV ->
                    if (right.toInt() == 0)
                        throw DivisionByZeroError(exp)
                    else
                        Value(INT, left.toInt() / right.toInt())

                ArithmeticOperator.MOD ->
                    if (right.toDouble() == 0.0)
                        throw DivisionByZeroError(exp)
                    else if (left.type == DOUBLE || right.type == DOUBLE)
                        Value(DOUBLE, left.toDouble() % right.toDouble())
                    else
                        Value(INT, left.toInt() % right.toInt())

                else -> {
                    val l: Double = left.toDouble()
                    val r: Double = right.toDouble()
                    val res = when (operator) {
                        ArithmeticOperator.ADD -> l + r
                        ArithmeticOperator.SUB -> l - r
                        ArithmeticOperator.MUL -> l * r
                        ArithmeticOperator.DIV ->
                            if (r == 0.0) throw DivisionByZeroError(exp)
                            else l / r

                        else -> unsupported()
                    }

                    if (type == INT) Value(type, res.toInt()) else Value(
                        type,
                        res
                    )
                }
            }

            is RelationalOperator -> when (operator) {
                RelationalOperator.EQUAL ->
                    if (left.isNumber && right.isNumber)
                        Value(BOOLEAN, left.toDouble() == right.toDouble())
                    else if (left is IReference<*> && right is IReference<*>)
                        Value(BOOLEAN, left.target === right.target)
                    else
                        Value(BOOLEAN, left.value == right.value)

                RelationalOperator.DIFFERENT ->
                    if (left.isNumber && right.isNumber)
                        Value(BOOLEAN, left.toDouble() != right.toDouble())
                    else if (left is IReference<*> && right is IReference<*>)
                        Value(BOOLEAN, left.target !== right.target)
                    else
                        Value(BOOLEAN, left.value != right.value)

                else -> {
                    val l: Double = left.toDouble()
                    val r: Double = right.toDouble()
                    val res = when (operator) {
                        RelationalOperator.SMALLER -> l < r
                        RelationalOperator.SMALLER_EQUAL -> l <= r
                        RelationalOperator.GREATER -> l > r
                        RelationalOperator.GREATER_EQUAL -> l >= r
                        else -> unsupported()
                    }
                    Value(type, res)
                }
            }

            is LogicalOperator -> Value(
                type, when (operator) {
                    LogicalOperator.AND -> left.toBoolean() && right.toBoolean()
                    LogicalOperator.OR -> left.toBoolean() || right.toBoolean()
                    LogicalOperator.XOR -> left.toBoolean() xor right.toBoolean()
                    else -> unsupported()
                }
            )

            else -> unsupported()
        }
    }
}


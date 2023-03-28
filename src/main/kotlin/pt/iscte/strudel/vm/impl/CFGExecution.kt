package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IBranchNode
import pt.iscte.strudel.model.cfg.IStatementNode
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.UnaryOperator
import pt.iscte.strudel.vm.*


class ProcedureExecution(
    val vm: IVirtualMachine,
    val procedure: IProcedure,
    vararg val arguments: IValue
) {
    val cfg = procedure.createCFG() // TODO optimize
    private var ip = cfg.entryNode.next
    var returnValue: IValue? = null

    val instructionPointer: IProgramElement?
        get() = ip?.element

    val currentExpression: IProgramElement?
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

    fun init() {
        vm.callStack.newFrame(procedure, arguments.toList())
    }

    fun run(): IValue? {
        init()
        val args = arguments.toList()
        vm.listeners.forEach { it.procedureCall(procedure, args) }
        while (!isOver())
            step()
        vm.listeners.forEach { it.procedureEnd(procedure, args, returnValue) }
        return returnValue
    }

    fun isOver() = ip == cfg.exitNode

    fun step() {
        while (expStack.isNotEmpty() && expStack.top is ILiteral)
            evaluate(expStack.pop())

        if (expStack.isNotEmpty()) {
            evaluate(expStack.pop())
        } else {
            when (ip) {
                is IStatementNode -> {
                    if (execute((ip as IStatementNode).element))
                        ip = (ip as IStatementNode).next
                }
                is IBranchNode -> {
                    val branch = ip as IBranchNode
                    if (valStack.isEmpty())
                        evaluateStep(branch.element.guard)
                    else
                        ip = if (valStack.pop().isTrue) {
                            if (branch.element is ILoop) {
                                vm.listeners.forEach { l -> l.loopIteration(branch.element as ILoop) }
                            }
                            branch.alternative
                        } else
                            branch.next
                }
            }
            if (ip == cfg.exitNode)
                vm.callStack.terminateTopFrame()
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
            for(i in e.parts.lastIndex downTo 0)
                evaluateStep(e.parts[i])

    }

    private fun execute(s: IStatement): Boolean {
        when (s) {
            is IVariableAssignment -> eval(s.expression)?.let {
                vm.callStack.topFrame.variables[s.target] = it
                vm.listeners.forEach { l -> l.variableAssignment(s, it) }
                return true
            }

            is IArrayElementAssignment -> eval(s.arrayAccess.target, s.arrayAccess.index, s.expression)?.let {
                val (array, index, value) = it
                val i = index.toInt()
                if (i < 0 || i >= ((array as IReference<IArray>).target).length)
                    throw ArrayIndexError(s.arrayAccess, i, s.arrayAccess.index)

                (array.target as IArray).setElement(i, value)
                vm.listeners.forEach { l -> l.arrayElementAssignment(s, i, value) }
                return true
            }

            is IRecordFieldAssignment -> eval(s.target, s.expression)?.let {
                val (target, value) = it
                ((target as IReference<IRecord>).target).setField(s.field, value)
                vm.listeners.forEach { l -> l.fieldAssignment(s, value) }
                return true
            }

            is IReturn ->
                if (s.expression != null) {
                    val e = eval(s.expression!!)
                    e?.let {
                        returnValue = it
                        vm.callStack.topFrame.returnValue = it
                        vm.listeners.forEach { l -> l.returnCall(s) }
                    }
                    return e != null
                } else {
                    vm.callStack.topFrame.returnValue = NULL
                    vm.listeners.forEach { it.returnCall(s) }
                    return true
                }

            is IProcedureCall -> eval(*s.arguments.reversed().toTypedArray())?.reversed()?.let {
                if (s.procedure.isForeign) {
                    (s.procedure as ForeignProcedure).run(vm, it)
                } else {
                    val call = ProcedureExecution(vm, s.procedure as IProcedure, *it.toTypedArray());
                    call.run()
                }
                return true
            }

            is IBreak, is IContinue -> {
                return true
            }

            else -> {
                System.err.println("not handled: $s")
                //vm.listeners.forEach { it.statementExecution(s) }
                return true
            }
        }
        return false
    }

    private fun evaluate(exp: IExpression) {
        when (exp) {
            is ILiteral -> {
                val lit = when(exp.type) {
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
                    throw RuntimeError(RuntimeErrorType.NONINIT_VARIABLE, exp, "variable not initialized")
                else
                    valStack.push(v)
            }

            is IArrayAllocation -> eval(exp.dimensions)?.let { dims ->
                dims.forEachIndexed { i, d ->
                    if (d.toInt() < 0)
                        throw NegativeArraySizeError(exp, d.toInt(), exp.dimensions[i])
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
                        (arrayRef.target as IArray).setElement(i, vm.allocateArray(exp.componentType, d.toInt()))
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
                if(valStack.size < 2) {
                    evaluateStep(exp.rightOperand)
                    evaluateStep(exp.leftOperand)
                }
                val right = valStack.pop()
                val left = valStack.pop()
                valStack.push(binoperation(exp.operator, exp.type, left, right))
            }

            is IUnaryExpression -> eval(exp.operand)?.let {
                valStack.push(unopearation(exp.operator, exp.type, it))
            }

            is IProcedureCallExpression -> {
                eval(*exp.arguments.reversed().toTypedArray())?.reversed()?.let {
                    if (exp.procedure.isForeign) {
                        val ret = (exp.procedure as ForeignProcedure).run(vm, it)
                        ret?.let {
                            valStack.push(ret)
                        }
                    } else {
                        val call = ProcedureExecution(vm, exp.procedure as IProcedure, *it.toTypedArray());
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

    private fun unopearation(operator: IUnaryOperator, type: IType, value: IValue): IValue =
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

    private fun binoperation(operator: IBinaryOperator, type: IType, left: IValue, right: IValue): IValue {
        if (operator is ArithmeticOperator) {
            if (operator == ArithmeticOperator.IDIV) return Value(INT, left.toInt() / right.toInt())
            if (operator == ArithmeticOperator.MOD) return Value(INT, left.toInt() % right.toInt())

            val l: Double = left.toDouble()
            val r: Double = right.toDouble()
            var res = when (operator) {
                ArithmeticOperator.ADD -> l + r
                ArithmeticOperator.SUB -> l - r
                ArithmeticOperator.MUL -> l * r
                ArithmeticOperator.DIV -> l / r
                else -> throw UnsupportedOperationException(operator.toString())
            }

            return if (type == INT) Value(type, res.toInt()) else Value(type, res)
        } else if (operator is RelationalOperator) {
            if (operator == RelationalOperator.EQUAL) return Value(BOOLEAN, left.value == right.value)
            if (operator == RelationalOperator.DIFFERENT) return Value(BOOLEAN, left.value != right.value)

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

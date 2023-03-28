package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.*

class ExpressionEvaluator(
    private var expression: IExpression,
    private val callStack: ICallStack) :
    IExpressionEvaluator {

    private val parts: List<IExpression>
    private val partial: MutableList<Any>
    private var next: Int
    private var result: IValue?

    init {
        parts = if(expression is ICompositeExpression) (expression as ICompositeExpression).parts else emptyList()
        partial = ArrayList(parts.size)
        for (e in parts) {
            if (e is ISimpleExpression) partial.add(e) else partial.add(ExpressionEvaluator(e, callStack))
        }
        next = 0
        result = null
    }

    override fun toString(): String {
        return "$expression = $parts * $next"
    }

    override val isComplete: Boolean
        get() = result != null

    override val value: IValue
        get() {
            check(isComplete)
            return result!!
        }

    override fun currentExpression(): IExpression? {
        check(!isComplete)
        return if (next == parts.size) expression else if (partial[next] is ISimpleExpression) partial[next] as ISimpleExpression else (partial[next] as ExpressionEvaluator).currentExpression()
    }

    //@Throws(RuntimeError::class)
    override fun evaluate(): IValue {
        while (!isComplete) step()
        return value
    }

    //@Throws(RuntimeError::class)
    override fun step(): IExpressionEvaluator.Step? {
        check(!isComplete)
        return if (next == parts.size) {
            val values: MutableList<IValue> = ArrayList()
            partial.forEach { p: Any -> values.add(p as IValue) }
            //result = callStack.topFrame.evaluate(expression, values)
            if (result == null) {
                expression = ProcedureReturnExpression((expression as IProcedureCall).procedure)
            }
            IExpressionEvaluator.Step((if (result == null) null else expression)!!, result!!)
        } else {
            if (partial[next] is ISimpleExpression) {
                val exp = partial[next] as ISimpleExpression
                //val r = callStack.topFrame.evaluate(exp, emptyList())
                val r = NULL
                val step = IExpressionEvaluator.Step(parts[next], r)
                partial[next] = r
                next++
                step
            } else {
                val eval = partial[next] as ExpressionEvaluator
                if (eval.isComplete) {
                    val r = eval.value
                    val step = IExpressionEvaluator.Step(parts[next], r)
                    partial[next] = r
                    next++
                    step
                } else {
                    eval.step()
                }
            }
        }
    }

    internal class ProcedureReturnExpression(val procedure: IProcedureDeclaration) : ISimpleExpression, IEvaluable {
        override val type: IType
        get() = procedure.returnType

        override fun includes(variable: IVariableDeclaration<*>): Boolean {
            return false
        }

       //@Throws(RuntimeError::class)
        override fun evalutate(values: List<IValue>, stack: ICallStack): IValue {
            return stack.lastTerminatedFrame!!.returnValue ?: NULL
        }

        override fun getProperty(key: String): Any {
            throw UnsupportedOperationException()
        }

        override fun setProperty(key: String, value: Any?) {}
        override fun conditional(trueCase: IExpression, falseCase: IExpression): IConditionalExpression {
           throw UnsupportedOperationException()
        }

        override fun length(): IArrayLength {
            throw UnsupportedOperationException()
        }

        override fun element(indexes: IExpression): IArrayAccess {
            throw UnsupportedOperationException()
        }

        override fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression {
            throw UnsupportedOperationException()
        }

        override fun cloneProperties(e: IProgramElement) {
            throw UnsupportedOperationException()
        }
    }


}
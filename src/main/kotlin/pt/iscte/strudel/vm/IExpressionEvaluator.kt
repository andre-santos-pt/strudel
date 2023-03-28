package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IExpression


interface IExpressionEvaluator {
    val isComplete: Boolean

    //@Throws(RuntimeError::class)
    fun evaluate(): IValue
    val value: IValue

    //@Throws(RuntimeError::class)
    fun step(): Step?
    fun currentExpression(): IExpression?

    class Step(val expression: IExpression, val value: IValue) {
        override fun toString(): String {
            return "$expression = $value"
        }
    }
}
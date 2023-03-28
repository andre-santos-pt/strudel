package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.IUnaryExpression
import pt.iscte.strudel.model.IUnaryOperator


internal class UnaryExpression(
    override val operator: IUnaryOperator,
    override val operand: IExpression)
    : Expression(), IUnaryExpression {

    override val type: IType
        get() = operator.getResultType(operand)

    override val parts: List<IExpression> = listOf(operand)

    override fun toString() = "${operator.id}$operand"
}
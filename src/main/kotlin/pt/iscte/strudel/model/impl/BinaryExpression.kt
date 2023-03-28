package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IBinaryExpression
import pt.iscte.strudel.model.IBinaryOperator
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IType


internal class BinaryExpression(override val operator: IBinaryOperator, val left: IExpression, val right: IExpression) : Expression(),
    IBinaryExpression {
    override val parts: List<IExpression> = listOf(left, right)

    override var leftOperand: IExpression = left

    override var rightOperand: IExpression = right

    override val type: IType
        get() = operator.getResultType(leftOperand, rightOperand)

    override fun toString() = "($left $operator $right)"
}
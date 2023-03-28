package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.BinaryExpression


val IBinaryOperator.isLogical get() = LogicalOperator.values().contains(this)

// TODO step eval
enum class LogicalOperator(override var id: String?) : IBinaryOperator {
    AND("&&"),
    OR("||"),
    XOR("^");

    override fun getResultType(left: IExpression, right: IExpression): IType {
        return BOOLEAN
    }

    override fun isValidFor(left: IExpression, right: IExpression): Boolean {
        return left.type.isBoolean && right.type.isBoolean
    }

    override fun on(leftOperand: IExpression, rightOperand: IExpression): IBinaryExpression {
        return BinaryExpression(this, leftOperand, rightOperand)
    }

    override fun setProperty(key: String, value: Any?) {
        check(false) { "unsupported" }
    }

    override fun getProperty(key: String): Any? {
        check(false) { "unsupported" }
        return null
    }

    override fun cloneProperties(e: IProgramElement) {
        check(false) { "unsupported" }
    }

    override fun toString(): String = id ?: name

}
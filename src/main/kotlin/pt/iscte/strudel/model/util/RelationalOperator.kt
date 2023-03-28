package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.BinaryExpression


val IBinaryOperator.isRelational get() = RelationalOperator.values().contains(this)

enum class RelationalOperator(override var id: String?) : IBinaryOperator {
    EQUAL("=="),
    DIFFERENT("!="),
    GREATER(">"),
    GREATER_EQUAL(">="),
    SMALLER("<"),
    SMALLER_EQUAL("<=");

    override fun getResultType(left: IExpression, right: IExpression): IType = BOOLEAN

    override fun isValidFor(left: IExpression, right: IExpression): Boolean {
        return if (this == EQUAL || this == DIFFERENT) left.isNull || right.isNull || left.type == right.type else left.type.isNumber && right.type.isNumber
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
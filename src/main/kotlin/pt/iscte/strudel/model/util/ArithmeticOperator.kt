package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.BinaryExpression


val IBinaryOperator.isArithmetic
    get() = ArithmeticOperator.values().contains(this)

enum class ArithmeticOperator (
    override var id: String?,
) : IBinaryOperator {
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    IDIV("/"),
    MOD("%"),
    XOR("^");

    override fun isValidFor(left: IExpression, right: IExpression) =
        if (this == XOR) left.type == INT && right.type == INT
        else left.type.isNumber && right.type.isNumber
                // || (left.type == INT || left.type == CHAR &&

    override fun getResultType(left: IExpression, right: IExpression): IType {
        //require(isValidFor(left, right))
        return getDataType(left.type, right.type)
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

    //    @Throws(ExecutionError::class)
//    override fun apply(left: IValue, right: IValue): IValue {
//        val type = getDataType(left.getType(), right.getType())
//        val obj: BigDecimal = f(left.getValue() as BigDecimal, right.getValue() as BigDecimal)
//        return Value.create(type, obj)
//    }


    private fun getDataType(left: IType, right: IType): IType {
        return if (left == INT && right == INT) INT
        else if (left == DOUBLE && right == INT) DOUBLE
        else if (left == INT && right == DOUBLE) DOUBLE
        else if (left == DOUBLE && right == DOUBLE) DOUBLE
        else UnboundType()
    }
}
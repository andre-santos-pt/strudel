package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayAccess
import pt.iscte.strudel.model.impl.BinaryExpression
import pt.iscte.strudel.model.impl.Literal

fun IExpression.toText(): String =
    if(this is IBinaryExpression)
        "${leftOperand.toText()} ${operator.id} ${rightOperand.toText()}"
    else if(this is IArrayAccess)
        "${target.toText()}[${index.toText()}]"
    else
        toString()

fun IExpression.isIntLiteral() = this is ILiteral && type == INT

fun IExpression.toInt() = (this as ILiteral).stringValue.toInt()

fun IExpression.solveIntArithmeticConstants(): IExpression =
    if(this is IBinaryExpression && operator.isArithmetic && leftOperand.isIntLiteral() && rightOperand.isIntLiteral()) {
        val left = leftOperand.toInt()
        val right = rightOperand.toInt()

        val res = when(operator as ArithmeticOperator) {
            ArithmeticOperator.ADD -> left + right
            ArithmeticOperator.SUB -> left - right
            ArithmeticOperator.MUL -> left * right
            ArithmeticOperator.DIV, ArithmeticOperator.IDIV -> left / right
            ArithmeticOperator.MOD -> left % right
        }
        Literal(INT, res.toString())
    }
    else if(this is IBinaryExpression)
        BinaryExpression(operator, leftOperand.solveIntArithmeticConstants(), rightOperand.solveIntArithmeticConstants())
    else if(this is IArrayAccess)
        ArrayAccess(target, index.solveIntArithmeticConstants())
    else
        this

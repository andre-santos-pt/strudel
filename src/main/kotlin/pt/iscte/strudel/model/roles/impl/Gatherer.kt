package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IGatherer
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.isArithmetic

class Gatherer(variable: IVariableDeclaration<*>) : IGatherer {
    override val operation: IGatherer.Operation
    override val accumulationExpression: IExpression
    private val expressionList: List<IProgramElement>

    init {
        require(isGatherer(variable))
        val v = Visitor(variable)
        variable.ownerProcedure.accept(v)
        expressionList = v.expressionList
        operation = v.operator!!
        accumulationExpression = v.accumulationExpression!!
    }

    override fun toString(): String {
        return "$name ($operation)"
    }

    private class Visitor(val variable: IVariableDeclaration<*>) : IBlock.IVisitor {
        var expressionList: MutableList<IProgramElement> = ArrayList<IProgramElement>()
        var accumulationExpression: IExpression? = null
        var allSameAcc = true
        var operator: IGatherer.Operation? = null
        var first = true


        override fun visit(assignment: IVariableAssignment): Boolean {
            if (assignment.target === variable) {
                if (first) {
                    first = false
                } else {
                    val op = getAccumulationOperator(assignment)
                    if (op == null || operator != null && op !== operator) {
                        allSameAcc = false
                    } else {
                        operator = op
                        expressionList.add(assignment)
                        val e: IBinaryExpression = assignment.expression as IBinaryExpression
                        accumulationExpression =
                            if (e.leftOperand.isSame(assignment.target.expression())) {
                                e.rightOperand
                            } else {
                                e.leftOperand
                            }
                    }
                }
            }
            return false
        }
    }

    override val expressions: List<IProgramElement>
        get() = expressionList

    companion object {
        fun isGatherer(variable: IVariableDeclaration<*>): Boolean {
            val visitor = Visitor(variable)
            variable.ownerProcedure.accept(visitor)
            return visitor.allSameAcc && visitor.operator != null
        }

        private fun getAccumulationOperator(variable: IVariableAssignment): IGatherer.Operation? {
            val expression: IExpression = variable.expression
            if (expression is IBinaryExpression) {
                val e: IBinaryExpression = expression as IBinaryExpression
                val left: IExpression = e.leftOperand
                val right: IExpression = e.rightOperand
                if (e.operator.isArithmetic &&
                    (left is IVariableExpression && left.variable === variable.target && right !is ILiteral ||
                            right is IVariableExpression && right.variable === variable.target && left !is ILiteral)
                ) return match(e.operator)
            }
            return null
        }

        private fun match(op: IBinaryOperator): IGatherer.Operation? =
            when (op) {
                ArithmeticOperator.ADD -> IGatherer.Operation.ADD
                ArithmeticOperator.SUB -> IGatherer.Operation.SUB
                ArithmeticOperator.MUL -> IGatherer.Operation.MUL
                ArithmeticOperator.DIV, ArithmeticOperator.IDIV -> IGatherer.Operation.DIV
                else -> null
            }
    }
}
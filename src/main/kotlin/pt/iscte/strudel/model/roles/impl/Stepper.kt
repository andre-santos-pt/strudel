package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IStepper
import pt.iscte.strudel.model.util.*

class Stepper(variable: IVariableDeclaration<*>) : IStepper {
    override val direction: IStepper.Direction?
    override val stepSize: Int
    override val expressions: List<IProgramElement>
    override val initializationValue: IExpression?
    override val cycleLimit: IExpression?
    override val isIteratingWholeArray: Boolean

    init {
        require(isStepper(variable))
        val v = Visitor(variable)
        variable.ownerProcedure.accept(v)
        direction = v.direction
        stepSize = v.stepSize
        expressions = v.expressionList
        initializationValue = v.initializationValue
        cycleLimit = v.upperLimit
        isIteratingWholeArray = v.isIteratingWholeArray
    }

    override fun toString(): String {
        return "$name($direction)"
    }

    private class Visitor(val `var`: IVariableDeclaration<*>) : IBlock.IVisitor {
        var expressionList: MutableList<IProgramElement> = ArrayList()
        var initializationValue: IExpression? = null
        var upperLimit: IExpression? = null
        var isIteratingWholeArray = false
        var first = true
        var direction: IStepper.Direction? = null
        var stepSize = Int.MIN_VALUE
        var isValid = true // true until proven otherwise
        override fun visit(assignment: IVariableAssignment): Boolean {
            if (assignment.target == `var` && isValid) {
                if (first) {
                    initializationValue = assignment.expression
                    first = !first
                } else {
                    // check direction and size of step(steps have to be of same size / direction)
                    val dir = getDirection(assignment)
                    if (assignment.parent.parent is ILoop) getConditionBoundaries(assignment)
                    if (dir == null || direction != null && dir !== direction) isValid = false else direction = dir
                }
            }
            return false
        }

        private fun getConditionBoundaries(assignment: IVariableAssignment) {
            val loop = assignment.parent.parent as ILoop
            val guard = loop.guard
            val list: MutableList<IBinaryExpression> = ArrayList()
            if (guard is IBinaryExpression) {
                getGuardRelationalExpressions(list, guard)
            }
            if (!list.isEmpty()) {
                for (ex in list) {
                    val leftEx = ex.leftOperand
                    val rightEx = ex.rightOperand
                    var leftVariableExpression = false
                    if (leftEx is IVariableExpression && leftEx.isSame(`var`.expression())) {
                        leftVariableExpression = true
                        upperLimit = rightEx
                    }
                    if (rightEx is IVariableExpression && rightEx.isSame(`var`.expression())) {
                        upperLimit = leftEx
                    }
                    if (upperLimit != null) {
                        checkIfIteratesWholeArray(ex.operator, leftVariableExpression)
                        break
                    }
                }
            }
        }

        private fun checkIfIteratesWholeArray(op: IBinaryOperator, leftVariableExpression: Boolean) {
            val leftEx: IExpression?
            val rightEx: IExpression?
            if (leftVariableExpression) {
                leftEx = initializationValue
                rightEx = upperLimit
            } else {
                leftEx = upperLimit
                rightEx = initializationValue
            }
            if (leftEx!!.isSame(INT.literal(0))) {
                if (op == RelationalOperator.SMALLER && rightEx is IArrayLength && stepSize == 1) isIteratingWholeArray =
                    true
                if (op == RelationalOperator.DIFFERENT && rightEx is IArrayLength && stepSize == 1) isIteratingWholeArray =
                    true
                if ((op == RelationalOperator.SMALLER_EQUAL && rightEx is IBinaryExpression
                            && rightEx.leftOperand is IArrayLength) && rightEx.operator == ArithmeticOperator.SUB && rightEx.rightOperand.isSame(
                        INT.literal(1)
                    ) && stepSize == 1
                ) isIteratingWholeArray = true
            }
            if (leftEx is IArrayLength) {
                if (op == RelationalOperator.GREATER && rightEx!!.isSame(INT.literal(0)) && stepSize == -1) {
                    isIteratingWholeArray = true
                }
                if (op == RelationalOperator.DIFFERENT && rightEx!!.isSame(INT.literal(0)) && stepSize == -1) {
                    isIteratingWholeArray = true
                }
            }
            if ((leftEx is IBinaryExpression && leftEx.leftOperand is IArrayLength) && leftEx.operator == ArithmeticOperator.SUB && leftEx.rightOperand.isSame(
                    INT.literal(1)
                )
            ) {
                if (op == RelationalOperator.GREATER_EQUAL && rightEx!!.isSame(
                        INT.literal(0)
                    ) && stepSize == -1
                ) {
                    isIteratingWholeArray = true
                }
            }
        }

        private fun getGuardRelationalExpressions(list: MutableList<IBinaryExpression>, expression: IBinaryExpression) {
            if (expression.operator.isLogical) {
                val leftEx = expression.leftOperand
                val rightEx = expression.rightOperand
                if (leftEx is IBinaryExpression) {
                    if (leftEx.operator.isLogical) {
                        getGuardRelationalExpressions(list, leftEx)
                    }
                    if (leftEx.operator.isRelational) {
                        list.add(leftEx)
                    }
                }
                if (rightEx is IBinaryExpression) {
                    if (rightEx.operator.isLogical) {
                        getGuardRelationalExpressions(list, rightEx)
                    }
                    if (rightEx.operator.isRelational) {
                        list.add(rightEx)
                    }
                }
            }

            list.add(expression)

            return
        }

        private fun getDirection(variable: IVariableAssignment): IStepper.Direction? { // Check step size / direction
            val expression = variable.expression
            if (expression is IBinaryExpression) {
                val be = expression
                val left = be.leftOperand // left e right -> ex: var = left + right
                val right = be.rightOperand
                expressionList.add(variable)
                if ((be.operator === ArithmeticOperator.ADD || be.operator === ArithmeticOperator.SUB) &&
                    left is IVariableExpression && left.variable == variable.target && isConstant(right)
                ) {
                    return getDirectionHelper(right as ILiteral, be)
                } else if (be.operator == ArithmeticOperator.ADD || be.operator === ArithmeticOperator.SUB &&
                    isConstant(left) &&
                    right is IVariableExpression &&
                    right.variable == variable.target
                ) {
                    return getDirectionHelper(left as ILiteral, be)
                }
            }
            return null
        }

        private fun isConstant(e: IExpression): Boolean {
            return e is ILiteral && e.type == INT ||
                    e is IUnaryExpression && e.operator == UnaryOperator.MINUS && e.operand is ILiteral
        }

        private fun getDirectionHelper(i: ILiteral, be: IBinaryExpression): IStepper.Direction? {
            val step = i.stringValue.toInt()
            if (stepSize != Int.MIN_VALUE && step != stepSize) return null // step size must always be the same
            else if (stepSize == Int.MIN_VALUE) stepSize = step
            return calculateDirection(be.operator, step)
        }

        private fun calculateDirection(
            op: IBinaryOperator,
            step: Int
        ): IStepper.Direction { // does not check if step == 0
            return if (op == ArithmeticOperator.ADD && step > 0 || op == ArithmeticOperator.SUB && step < 0) IStepper.Direction.INC else IStepper.Direction.DEC
        }
    }

    companion object {
        fun isStepper(variable: IVariableDeclaration<*>): Boolean {
            val v = Visitor(variable)
            variable.ownerProcedure.accept(v)
            return v.isValid && v.direction != null
        }
    }
}
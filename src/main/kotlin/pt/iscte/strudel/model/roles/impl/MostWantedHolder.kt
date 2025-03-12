package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IMostWantedHolder
import pt.iscte.strudel.model.roles.IMostWantedHolder.Objective
import pt.iscte.strudel.model.roles.IMostWantedHolder.VarPosition
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.isRelational

class MostWantedHolder(val targetVar: IVariableDeclaration<*>) : IMostWantedHolder {
    override val objective: Objective
    override val targetArray: IVariableExpression?
    var arrayEl: IArrayAccess?
    override val expressions: List<IProgramElement>

    init {
        require(isMostWantedHolder(targetVar))
        val visitor = Visitor(targetVar)
        targetVar.ownerProcedure.accept(visitor)
        objective = visitor.objective
        targetArray = visitor.arrayVar
        arrayEl = visitor.arrayEl
        expressions = visitor.expressionList
    }

    private class Visitor(val targetVar: IVariableDeclaration<*>) : IBlock.IVisitor {
        var arrayVar: IVariableExpression? = null
        var arrayEl: IArrayAccess? = null
        var expressionList: MutableList<IProgramElement> = ArrayList()
        var initialValues: MutableMap<IVariableDeclaration<*>, IExpression> = HashMap()
        var iteratesWholeArray = false

        /**
         * Checks if the visited If is inside a while
         */
        var isIfInsideWhile = false

        /**
         * Checks if the While's has the iterator varible in its condition.
         */
        var isiteratorInWhileGuard = false

        /**
         * Checks if the visited If has an Assignment of the type TargetVar =
         * ArrayVar[Variable].
         */
        var isAssignmentCorrect = false
        var isOperatorValid = false
        var indexIterator: IExpression? = null
        var objective: Objective = Objective.UNDEFINED

        override fun visit(assignment: IVariableAssignment): Boolean {
            if (assignment.target.type == INT && !initialValues.containsKey(assignment.target)) {
                initialValues[assignment.target] = assignment.expression
            }
            return true
        }

        override fun visit(selection: ISelection): Boolean {
            if (selection.guard is IBinaryExpression) {
                val guard = selection.guard as IBinaryExpression
                var varPos = VarPosition.NONE
                if (guard.leftOperand is IVariableExpression && guard.leftOperand as IVariableExpression == targetVar.expression())
                    varPos = VarPosition.LEFT
                if (guard.rightOperand is IVariableExpression
                    && (guard.rightOperand as IVariableExpression).isSame(targetVar.expression())
                )
                    varPos = VarPosition.RIGHT
                val op: RelationalOperator = guard.operator as RelationalOperator
                if (varPos != VarPosition.NONE && op == RelationalOperator.GREATER
                    || op == RelationalOperator.SMALLER
                ) {
                    objective = getMHWObjective(guard, varPos)
                    expressionList.add(guard)
                    val parent: IProgramElement = selection.parent.parent
                    checkStatementConditions(selection)
                    checkWhileConditions(parent, indexIterator)
                }
            }
            return false
        }

        /**
         * Checks if certain conditions are true to discover if the target Variable is a
         * MostWantedHolder Condition 1: If the visited If is inside a While. Condition
         * 2: If the While's guard has the iterator variable.
         *
         * @param parent
         * @param aVar
         * @param indexIterator
         */
        fun checkWhileConditions(parent: IProgramElement?, indexIterator: IExpression?) {
            if (parent is ILoop) {
                isIfInsideWhile = true
                val parentGuard = parent.guard as IBinaryExpression
                checkBinaryExpressionCondition(parentGuard, indexIterator)
            }
        }

        fun checkBinaryExpressionCondition(binaryEx: IBinaryExpression, indexIterator: IExpression?) {
            val left = binaryEx.leftOperand
            val right = binaryEx.rightOperand
            if (left is IBinaryExpression && left.operator.isRelational)
                checkBinaryExpressionCondition(left, indexIterator)
            if (right is IBinaryExpression && right.operator.isRelational)
                checkBinaryExpressionCondition(right, indexIterator)
            if (binaryEx.operator.isRelational) {
                if (left is IVariableExpression && ArrayIndexIterator.isArrayIndexIterator(left.variable)) {
                    isiteratorInWhileGuard = true
                } else if (right is IVariableExpression && ArrayIndexIterator.isArrayIndexIterator(right.variable)) {
                    isiteratorInWhileGuard = true
                }
                if (isiteratorInWhileGuard) {
                    expressionList.add(binaryEx)
                }
            }
        }

        /**
         * Checks if certain conditions are true to discover if the target Variable is a
         * MostWantedHolder Condition 1: Checks if the visited If has a
         * VariableAssignment in which the target is the targetVariable and if the right
         * of the operator is the array Variable.
         *
         * @param parent
         * @param aVar
         */
        fun checkStatementConditions(expression: ISelection) {
            for (i in expression.block.children) {
                if (i is IVariableAssignment) {
                    val assignment = i
                    val target: IVariableDeclaration<*> = assignment.target
                    if (assignment.expression is IArrayAccess) {
                        val ex = assignment.expression as IArrayAccess
                        indexIterator = ex.index
                        arrayEl = ex
                        arrayVar = (assignment.expression as IArrayAccess).target as IVariableExpression
                        if (target === targetVar &&
                            ((ex.target as IVariableExpression).variable == arrayVar!!.variable)
                        ) {
                            isAssignmentCorrect = true
                            expressionList.add(assignment)
                        }
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return "$name($objective)"
    }

    companion object {
        fun isMostWantedHolder(variable: IVariableDeclaration<*>?): Boolean {
            if (variable != null) {
                val v = Visitor(variable)
                variable.ownerProcedure.accept(v)
                return v.isIfInsideWhile && v.isAssignmentCorrect
            }
            return false
        }

        fun getMHWObjective(assignment: IBinaryExpression, varPos: VarPosition): Objective {
            return if (assignment.leftOperand !is IBinaryExpression
                && assignment.rightOperand !is IBinaryExpression
            ) {
                if (varPos == VarPosition.RIGHT) match(
                    assignment.operator,
                    false
                ) else match(assignment.operator, true)
            } else Objective.UNDEFINED
        }

        // TODO buggy?
        fun match(op: IBinaryOperator, inverse: Boolean): Objective {
            if (!inverse)
                if (op === RelationalOperator.GREATER) return Objective.GREATER
            if (op === RelationalOperator.SMALLER) return Objective.SMALLER
            else if (op === RelationalOperator.GREATER) return Objective.SMALLER
            return if (op === RelationalOperator.SMALLER) Objective.GREATER else Objective.UNDEFINED
        }
    }
}
package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IOneWayFlag

class OneWayFlag(targetVar: IVariableDeclaration<*>) : IOneWayFlag {
    override val conditions: List<IProgramElement>
    override val assignment: IVariableAssignment

    init {
        require(isOneWayFlag(targetVar))
        val v = Visitor(targetVar)
        targetVar.ownerProcedure.accept(v)
        conditions = v.conditionList
        assignment = v.assignment!!
    }

    private class Visitor(val targetVar: IVariableDeclaration<*>) : IBlock.IVisitor {
        var isValid = false
        private var first = true
        private var initialValue: IExpression? = null
        val conditionList: MutableList<IProgramElement> = ArrayList()
        var assignment: IVariableAssignment? = null
        override fun visit(expression: IVariableAssignment): Boolean {
            if (expression.target.expression().isSame(targetVar.expression())) {
                if (first) {
                    first = false
                    isValid = true
                    initialValue = expression.expression
                } else {
                    if (assignment == null) assignment = expression
                    if (expression.expression.isSame(initialValue!!)) isValid = false
                    if (expression.parent.parent is ISelection) {
                        conditionList.add((expression.parent.parent as ISelection).guard)
                    }
                }
            }
            return false
        }
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun isOneWayFlag(variable: IVariableDeclaration<*>?): Boolean {
            if (variable != null) {
                if (variable.type != BOOLEAN) return false
                val v = Visitor(variable)
                variable.ownerProcedure.accept(v)
                return v.isValid
            }
            return false
        }
    }
}
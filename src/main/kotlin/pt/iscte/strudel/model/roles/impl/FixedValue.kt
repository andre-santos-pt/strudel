package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IFixedValue

class FixedValue(variable: IVariableDeclaration<*>) : IFixedValue {
    override val isModified: Boolean
    val expressions: List<IProgramElement>

    init {
        require(isFixedValue(variable))
        val visitor = Visitor(variable)
        variable.ownerProcedure.accept(visitor)
        isModified = visitor.isModified
        expressions = visitor.expressionList
    }

    override fun toString(): String {
        return if (isModified) "$name array that has been modified" else name
    }

    private class Visitor(  //acrescentar para objetos (records)
        val v: IVariableDeclaration<*>
    ) : IBlock.IVisitor {
        var expressionList: MutableList<IProgramElement> = ArrayList()
        var isValid = true //valid until assigned
        var first //if is first assignment
                = false
        var isModified //true if variable is an array and is modified internally
                = false

        init {
            first = !v.ownerProcedure.parameters.contains(v)
        }

        override fun visit(assignment: IVariableAssignment): Boolean {
            if (assignment.target == v) {
                isModified = false
                if (first) {
                    first = false
                } else if (isValid)
                    isValid = false
            }
            return false
        }

        override fun visit(assignment: IArrayElementAssignment): Boolean {
            if (assignment.arrayAccess.target is IVariableExpression && (assignment.arrayAccess.target as IVariableExpression).variable == v) {
                isModified = true
                expressionList.add(assignment)
            }
            return false
        }
    }

    companion object {
        fun isFixedValue(variable: IVariableDeclaration<*>): Boolean {
            val v = Visitor(variable)
            variable.ownerProcedure.accept(v)
            return v.isValid
        }
    }
}
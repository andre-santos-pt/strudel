package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IFunctionClassifier

class FunctionClassifier(private val method: IProcedure) : IFunctionClassifier {
    private val assignments: MutableList<IProgramElement>
    override var classification: IFunctionClassifier.Status = IFunctionClassifier.Status.UNDEFINED
    private val modifiedVariables: MutableList<IVariableDeclaration<*>>

    init {
        classification =
            if (method.returnType.isVoid || method.returnType.isUnbound) IFunctionClassifier.Status.UNDEFINED else IFunctionClassifier.Status.FUNCTION
        modifiedVariables = ArrayList()
        assignments = ArrayList()
        for (variable in method.parameters) {
            if (variable.type is IReferenceType) {
                val v = Visitor(variable)
                method.accept(v)
                if (v.isMemoryValueChanged) {
                    modifiedVariables.add(variable)
                    classification = IFunctionClassifier.Status.PROCEDURE
                }
                if (!v.assignments.isEmpty()) assignments.addAll(v.assignments)
            }
        }
        val cv = CallVisitor()
        method.accept(cv)
        if (!cv.allFunctionCalls) classification = IFunctionClassifier.Status.PROCEDURE
    }

    internal inner class Visitor(val variable: IVariableDeclaration<*>) : IBlock.IVisitor {
        var isMemoryValueChanged = false
        var assignments: MutableList<IProgramElement> = ArrayList()

        override fun visit(assignment: IArrayElementAssignment): Boolean {
            var target = assignment.arrayAccess.target
            // TODO BUG
            while (target !is IVariableExpression) target = (target as IRecordFieldExpression).target
            if (target.isSame(variable)) {
                assignments.add(assignment)
                isMemoryValueChanged = true
            }
            return false
        }

        override fun visit(assignment: IRecordFieldAssignment): Boolean {
            var target = assignment.target

            // TODO BUG
            while (target is IRecordFieldExpression) {
                target = target.target
            }
            if (target is IVariableExpression && target.isSame(variable)) {
                assignments.add(assignment)
                isMemoryValueChanged = true
            }
            return false
        }
    }

    internal inner class CallVisitor : IBlock.IVisitor {
        var allFunctionCalls = true
        fun allFunctionCalls(): Boolean {
            return allFunctionCalls
        }

        private fun checkMethod(call: IProcedureCall) {
            // prevent recursion
            if (call.procedure !== method) {
                val c = FunctionClassifier(call.procedure as IProcedure)
                if (c.classification !== IFunctionClassifier.Status.FUNCTION) {
                    allFunctionCalls = false
                }
            }
        }

        override fun visit(call: IProcedureCall): Boolean {
            checkMethod(call)
            return true
        }

        override fun visit(exp: IProcedureCallExpression): Boolean {
            checkMethod(exp)
            return true
        }
    }

    override val expressions: List<IProgramElement>
        get() = assignments
    override val variables: List<IVariableDeclaration<*>>
        get() = modifiedVariables
}
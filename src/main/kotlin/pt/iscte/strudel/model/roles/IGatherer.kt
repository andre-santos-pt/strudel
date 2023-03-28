package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.roles.impl.Gatherer

interface IGatherer : IVariableRole {
    val operation: Operation

    // TODO
    //IVariable getSource();
    override val name: String
        get() = "Gatherer"

    val accumulationExpression: IExpression?

    /**
     * Returns a list containing expressions used to determine whether the variable is or not a Gatherer.
     * In Gatherer's case it's all IVariableAssignments type accumulations.
     * Useful for expression marking on Javardise.
     * @return
     */
    val expressions: List<IProgramElement>

    enum class Operation {
        ADD, SUB, MUL, DIV, MOD
    }
}
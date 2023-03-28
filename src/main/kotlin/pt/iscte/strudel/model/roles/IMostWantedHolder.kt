package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.roles.impl.MostWantedHolder

interface IMostWantedHolder : IVariableRole {
    val objective: Objective
    val targetArray: IVariableExpression?
    //IVariableExpression getIterator();
    /**
     * returns a list containing expressions used to determine whether the variable is or not a MostWantedHolder.
     * Useful for expression marking on Javardise.
     * Order in List: 1- Selection Guard, 2- Loop Guard, 3- Max/Min value assignment.
     * @return
     */
    val expressions: List<IProgramElement>

    override val name: String
        get() = "MostWantedHolder"

    enum class Objective {
        GREATER, SMALLER, UNDEFINED
    }

    enum class VarPosition {
        RIGHT, LEFT, NONE
    }
}
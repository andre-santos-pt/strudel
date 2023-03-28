package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.roles.impl.FunctionClassifier

interface IFunctionClassifier {
    enum class Status {
        FUNCTION, PROCEDURE, UNDEFINED
    }

    val classification: Status

    /**
     * @return Returns a list containing all assignments which are deemed to be the reason of why the method is considered a procedure
     * and returns an empty list if the method is a function.
     * Useful for expression marking on Javardise.
     */
    val expressions: List<IProgramElement>

    /**
     * @return a list containning the modified variables
     */
    val variables: List<IVariableDeclaration<*>>
}
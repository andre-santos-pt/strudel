package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.roles.impl.Stepper

interface IStepper : IVariableRole {
    val direction: Direction?
    val initializationValue: IExpression?

    /**
     * In case of the variable Stepper being used to iterate cycles of a loop, getUpperLimit will return a expression containning the upperLimit which will cause the loop to stop
     * e.g while(i < array.length) upperLimit = array.length
     * @return IExpression
     */
    val cycleLimit: IExpression?
    /**
     * Returns a list containing expressions used to determine whether the variable is or not a Stepper.
     * In Stepper's case it's all IVariableAssignments type incrementations/decrementation.
     * Order in List: insertion order.
     * Useful for expression marking on Javardise.
     * @return List<IProgramElement>
    </IProgramElement> */
    /**
     * Partially checks if the variable is being used to iterate the whole array.
     */
    val isIteratingWholeArray: Boolean
    val expressions: List<IProgramElement?>?
    val stepSize: Int

    //	IExpression getFirstValue();
    //	IExpression getBound();
    //	boolean isBoundInclusive()
    override val name: String
        get() = "Stepper"

    enum class Direction {
        INC, DEC
    }
}
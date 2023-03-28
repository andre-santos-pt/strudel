package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class VariableExpression(
    override val variable: IVariableDeclaration<*>
) : Expression(), IVariableExpression {

    override val type: IType
        get() = variable.type

    override fun toString(): String = variable.id ?: "\$${variable.ownerProcedure.variables.indexOf(variable)}"

}
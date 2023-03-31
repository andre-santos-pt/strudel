package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IPredefinedArrayAllocation
import pt.iscte.strudel.model.IType


internal class PredefinedArrayAllocation(
    override val componentType: IType,
    override val elements: List<IExpression>,
) : Expression(), IPredefinedArrayAllocation {

    override val parts: List<IExpression>
        get() = elements

    override fun toString(): String = elements.joinToString(prefix = "new ${componentType.id}[] {", separator = ", ", postfix = "}"){ "$it" }

}
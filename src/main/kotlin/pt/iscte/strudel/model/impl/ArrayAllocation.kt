package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IArrayType
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IType


internal class ArrayAllocation(
    override val componentType: IType,
    override  val dimensions: List<IExpression>,
) : Expression(), IArrayAllocation {

    override val type: IType = componentType.array(dimensions.size).reference()

    override val parts: List<IExpression>
        get() = dimensions

    override fun toString() = "new $componentType ${dimensions.joinToString(separator = "") { "[$it]" }}"
}
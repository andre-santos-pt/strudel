package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class ArrayLength(
     override val target: ITargetExpression,
) : Expression(), IArrayLength {

    override val type: IType
        get() = INT

    override val parts: List<IExpression> = listOf(target)

    override fun toString() = "$target.length"

}
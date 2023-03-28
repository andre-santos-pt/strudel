package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class ArrayAccess(
    override val target: ITargetExpression,
    override val index: IExpression
) : Expression(), IArrayAccess {

    override fun toString() = "$target[$index]"
}

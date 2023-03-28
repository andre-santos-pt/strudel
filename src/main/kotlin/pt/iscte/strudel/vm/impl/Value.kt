package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Value (override val type: IType, override val value: Any?) : IValue {
    override fun toString() = value.toString()
    override fun copy() = this
    override val isNull: Boolean = false

    override val isTrue: Boolean = value == true
    override val isFalse: Boolean = value == false
}
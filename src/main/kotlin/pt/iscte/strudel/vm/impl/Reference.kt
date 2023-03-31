package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.UnboundType
import pt.iscte.strudel.model.impl.ReferenceType
import pt.iscte.strudel.vm.IMemory
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Reference<T: IValue>(override var target: T) : IReference<T> {
    override val type: IType
        get() = ReferenceType(target.type)

    override val value: Any
        get() = target


    override fun copy(): IReference<T> {
        return Reference(target)
    }

    override fun toString(): String {
        return "$target"
    }

    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
}
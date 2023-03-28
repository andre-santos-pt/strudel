package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IReferenceType
import pt.iscte.strudel.model.IType

internal class ReferenceType(override val target: IType) : ProgramElement(), IReferenceType {
    override var id: String?
        get() = target.id
        set(id) {
            super<ProgramElement>.id = id
        }

    override fun toString(): String = id?: super.toString()

    override fun isSame(e: IProgramElement): Boolean =
        e is IReferenceType && e.target.isSame(target)

    override fun reference(): IReferenceType {
        throw UnsupportedOperationException()
    }
}
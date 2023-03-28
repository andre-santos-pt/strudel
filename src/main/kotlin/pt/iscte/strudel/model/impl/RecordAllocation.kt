package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IRecordAllocation
import pt.iscte.strudel.model.IRecordType
import pt.iscte.strudel.model.IType

internal class RecordAllocation(override val recordType: IRecordType) : Expression(), IRecordAllocation {
    override val type: IType
        get() = recordType.reference()

    override fun toString(): String = "new $type()"
}
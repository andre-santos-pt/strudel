package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IRecordType
import pt.iscte.strudel.model.IType


interface IHeapMemory {
    //@Throws(RuntimeError::class)
    fun allocateArray(baseType: IType, vararg dimensions: Int, sourceExp: IExpression? = null): IArray

    //@Throws(RuntimeError::class)
    fun allocateRecord(type: IRecordType, sourceExp: IExpression? = null): IRecord

    val memory: Int
}
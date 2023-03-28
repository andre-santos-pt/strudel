package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IRecordType
import pt.iscte.strudel.model.IVariableDeclaration


interface IRecord : IMemory {
    fun getField(field: IVariableDeclaration<IRecordType>): IValue
    fun setField(field: IVariableDeclaration<IRecordType>, value: IValue)

    fun addListener(listener: IListener)

    fun removeListener(listener: IListener)

    interface IListener {
        fun fieldChanged(field: IVariableDeclaration<IRecordType>, oldValue: IValue, newValue: IValue)
    }
}
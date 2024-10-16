package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IArrayType

const val ARRAY_OVERHEAD = 28

interface IArray : IMemory {

    val length: Int
    fun getElement(i: Int): IValue
    fun setElement(i: Int, value: IValue)
    override fun copy(): IArray

    override val memory: Int
        get() = ARRAY_OVERHEAD + length * (type as IArrayType).bytes

    var elements: List<IValue>

    fun addListener(listener: IListener)

    fun removeListener(listener: IListener)

    interface IListener {
        fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {}
        fun elementRead(index: Int, value: IValue) {}
    }
}
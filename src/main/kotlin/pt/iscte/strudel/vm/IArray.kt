package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IArrayType

const val ARRAY_OVERHEAD = 24

interface IArray : IMemory {

    val length: Int
    fun getElement(i: Int): IValue
    fun setElement(i: Int, value: IValue)
    override fun copy(): IArray

    override val memory: Int
        get() = length * (type as IArrayType).bytes + ARRAY_OVERHEAD

    fun setElements(vararg values: IValue) {
        require(values.size <= length)
        values.forEachIndexed {
            i, e -> setElement(i, e)
        }
    }

    fun addListener(listener: IListener)

    fun removeListener(listener: IListener)

    interface IListener {
        fun elementChanged(index: Int, oldValue: IValue, newValue: IValue)
    }
}
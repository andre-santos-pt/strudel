package pt.iscte.strudel.vm


interface IArray : IMemory {

//    override val memory: Int
//        get() = length * (type as IArrayType).componentType.
    val length: Int
    fun getElement(i: Int): IValue
    fun setElement(i: Int, value: IValue)
    override fun copy(): IArray

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
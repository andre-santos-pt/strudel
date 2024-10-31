package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.IArrayType
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Array(private val t: IArrayType, length: Int) : IArray {
    internal val array: kotlin.Array<IValue> = Array(length) { NULL }
    override val type get() = t

    private val listeners = mutableListOf<IArray.IListener>()

    override fun copy(): IArray {
        val copy = Array(type, array.size)
        for (i in array.indices) copy.array[i] = array[i]
        return copy
    }

    override val length: Int  get() = array.size

    override var elements: List<IValue>
        get() = array.toList()
        set(value) {
            require(value.size == length)
            for (i in value.indices) array[i] = value[i]
        }

    override fun getElement(i: Int): IValue {
        val elem = array[i]
        listeners.forEach {
            it.elementRead(i, elem)
        }
        return elem
    }

    override fun setElement(i: Int, value: IValue) {
        val old = array[i]
        array[i] = value
        listeners.forEach {
            it.elementChanged(i, old, value)
        }
    }

    override val value: Any
        get() = array

    override fun toString() = "[" + array.joinToString(", ") + "]"

    override val isNull: Boolean = false
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
    override val isNumber: Boolean = false


    override fun addListener(listener: IArray.IListener) {
       listeners.add(listener)
    }

    override fun removeListener(listener: IArray.IListener) {
       listeners.remove(listener)
    }

    override fun equals(other: Any?): Boolean {
        return other is Array && array.size == other.array.size &&
                array.mapIndexed { i, v -> v == other.array[i] }.all { it }
    }
}
package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.IArrayType
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Array(private val t: IArrayType, length: Int) : IArray {
    private val elements: kotlin.Array<IValue> = Array(length) { NULL }
    override val type get() = t

    private val listeners = mutableListOf<IArray.IListener>()

    override fun copy(): IArray {
        val copy = Array(type, elements.size)
        for (i in elements.indices) copy.elements[i] = elements[i]
        return copy
    }

    override val length: Int  get() = elements.size

    override fun getElement(i: Int): IValue {
        val elem = elements[i]
        listeners.forEach {
            it.elementRead(i, elem)
        }
        return elem
    }

    override fun setElement(i: Int, value: IValue) {
        val old = elements[i]
        elements[i] = value
        listeners.forEach {
            it.elementChanged(i, old, value)
        }
    }

    override val value: Any
        get() = elements

    override fun toString() = "[" + elements.joinToString(", ") + "]"

    override val isNull: Boolean = false
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false

    override fun addListener(listener: IArray.IListener) {
       listeners.add(listener)
    }

    override fun removeListener(listener: IArray.IListener) {
       listeners.remove(listener)
    }
}
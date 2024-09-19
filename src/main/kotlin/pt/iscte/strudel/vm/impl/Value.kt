package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Value (override val type: IType, override val value: Any?) : IValue {

    init {
        when (type) {
            INT -> assert(value is Int || value is Long)
            DOUBLE -> assert(value is Double || value is Float)
            CHAR -> assert(value is Char)
            BOOLEAN -> assert(value is Boolean)
        }
    }

    override fun toString() = value.toString()
    override fun copy() = this
    override val isNull: Boolean = false

    override val isTrue: Boolean = value == true
    override val isFalse: Boolean = value == false
}
package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NULL

internal class Value (override val type: IType, override val value: Any?) : IValue {

    init { // For peace of mind
        when (type) {
            INT -> assert(value is Int || value is Long) { "Cannot assign ${value!!::class.simpleName} value to $type" }
            DOUBLE -> assert(value is Double || value is Float) { "Cannot assign ${value!!::class.simpleName} value to $type" }
            CHAR -> assert(value is Char) { "Cannot assign ${value!!::class.simpleName} value to $type" }
            BOOLEAN -> assert(value is Boolean) { "Cannot assign ${value!!::class.simpleName} value to $type" }
        }
    }

    override fun toString() = value.toString()
    override fun copy() = this
    override val isNull: Boolean = false

    override val isTrue: Boolean = value == true
    override val isFalse: Boolean = value == false
}
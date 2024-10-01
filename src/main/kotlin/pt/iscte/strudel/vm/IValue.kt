package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.impl.Reference

object NULL : IValue {
    override val value = this
    override val type: IType get() = UnboundType()
    override fun toString() = "null"
    override fun copy() = this

    override val isNull: Boolean = true
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
    override val isNumber: Boolean = false
}

object VOID : IValue {
    override val value = null
    override val type: IType get() = UnboundType()
    override fun toString() = "void"
    override fun copy() = this
    override val isNull: Boolean = false
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
    override val isNumber: Boolean = false

}

interface IValue {

    // TODO value overflow error
    val type: IType
    val value: Any?
    fun copy(): IValue

    val isNull: Boolean

    val isTrue: Boolean

    val isFalse: Boolean

    val isNumber: Boolean

    val memory: Int
        get() = type.bytes

    fun toInt(): Int {
        //check(type === INT)
        return when (value) {
            is Int -> value as Int
            is Char -> (value as Char).code
            else -> (value as Double).toInt()
        }
    }

    fun toDouble(): Double {
        //check(type is INT || type is DOUBLE)
        return (value as Number).toDouble()
    }

    fun toBoolean(): Boolean {
        //check(type is BOOLEAN)
        return value as Boolean
    }

    fun toChar() : Char {
        return value as Char
    }

    fun reference(): IReference<IValue> = Reference(this)
}

interface IMemory : IValue
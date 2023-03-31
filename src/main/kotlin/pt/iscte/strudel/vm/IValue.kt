package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*

object NULL : IValue {
    override val value = null
    override val type: IType get() = UnboundType()
    override fun toString() = "null"
    override fun copy() = this

    override val isNull: Boolean = true
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
}

object VOID : IValue {
    override val value = null
    override val type: IType get() = UnboundType()
    override fun toString() = "void"
    override fun copy() = this
    override val isNull: Boolean = false
    override val isTrue: Boolean = false
    override val isFalse: Boolean = false
}

interface IValue {
    // TODO value overflow error
    val type: IType
    val value: Any?
    fun copy(): IValue

    val isNull: Boolean

    val isTrue: Boolean

    val isFalse: Boolean

    val memory: Int
        get() = 0 //type.memoryBytes

    fun toInt(): Int {
        //check(type === INT)
        return if(value is Int) value as Int else (value as Double).toInt()
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
}

interface IMemory : IValue
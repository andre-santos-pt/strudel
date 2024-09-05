package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.model.impl.ReferenceType

interface IType : IProgramElement {

    val bytes: Int

    val isVoid: Boolean
        get() = this === VOID
    val isUnbound: Boolean
        get() = this is UnboundType
    val isValueType: Boolean
        get() = this is IValueType<*>
    val isNumber: Boolean
        get() = this === INT || this === DOUBLE
    val isBoolean: Boolean
        get() = this === BOOLEAN
    val isCharacter: Boolean
        get() = this === CHAR

    val isReference: Boolean
        get() = this is IReferenceType
    val isArrayReference: Boolean
        get() = isReference && (this as IReferenceType).target is IArrayType
    val isRecordReference: Boolean
        get() = isReference && (this as IReferenceType).target is IRecordType

    val asArrayType: IArrayType
        get() = (this as IReferenceType).target as IArrayType

    val asRecordType: IRecordType
        get() = if (this is IRecordType) this else (this as IReferenceType).target as IRecordType
    fun reference(): IReferenceType =
        if(TypeCache.reference.containsKey(this))
            TypeCache.reference[this]!!
        else {
            val t = ReferenceType(this)
            TypeCache.reference[this] = t
            t
        }

    val defaultExpression: IExpression

    fun array(): IArrayType =
        if(TypeCache.array.containsKey(this))
            TypeCache.array[this]!!
        else {
            val t = ArrayType(this)
            TypeCache.array[this] = t
            t
        }

    fun array(n: Int): IArrayType {
        var a = array()
        (1 until n).forEach { _ ->
            a = a.array()
        }
        return a
    }
}


private object TypeCache {
    val array = mutableMapOf<IType, IArrayType>()
    val reference = mutableMapOf<IType, IReferenceType>()
}

object VOID : IType {

    override fun reference(): IReferenceType {
        throw RuntimeException("not valid")
    }

    override fun toString(): String {
        return "void"
    }

    override fun setProperty(key: String, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getProperty(key: String): Any? {
        TODO("Not yet implemented")
    }

    override var id: String? = "void"
        set(value) = check(false)

    override val defaultExpression: IExpression
        get() = NULL_LITERAL

    override fun cloneProperties(e: IProgramElement) {
        throw UnsupportedOperationException()
    }

    override val bytes: Int
        get() = 0
}

val ANY = UnboundType()

class UnboundType(override val defaultExpression: IExpression = NULL_LITERAL) : IType {
    override fun reference(): IReferenceType {
        TODO("Not yet implemented")
    }

    override fun setProperty(key: String, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getProperty(key: String): Any {
        TODO("Not yet implemented")
    }

    override fun cloneProperties(e: IProgramElement) {
        TODO("Not yet implemented")
    }

    override var id: String?
        get() = "Object"
        set(value) {}

    override fun isSame(e: IProgramElement): Boolean {
        return e is IType
    }

    override fun toString(): String = "Object"

    override val bytes: Int
        get() = TODO("Not yet implemented")

}


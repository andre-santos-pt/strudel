package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.model.impl.ReferenceType


interface ITypeProvider {

    fun getArrayType(componentType: IType): IArrayType

    fun getArrayType(componentType: IType, dimensions: Int): IArrayType {
        var at = getArrayType(componentType)
        (1 until dimensions).forEach { _ ->
            at = getArrayType(at)
        }
        return at
    }

    fun getReferenceType(targetType: IType): IReferenceType
}

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

    val isArray: Boolean
        get() = this is IArrayType
    val asArrayType: IArrayType
        get() = if (this is IArrayType) this else (this as IReferenceType).target as IArrayType

    val asRecordType: IRecordType
        get() = if (this is IRecordType) this else (this as IReferenceType).target as IRecordType

    val defaultExpression: IExpression

    fun reference(): IReferenceType

    fun array(): IArrayType

    fun array(n: Int): IArrayType {
        var a = array()
        (1 until n).forEach { _ ->
            a = a.array()
        }
        return a
    }
}

object VOID : IType {

    override fun reference(): IReferenceType {
        throw UnsupportedOperationException("cannot get reference to void type")
    }

    override fun array(): IArrayType {
        throw UnsupportedOperationException("cannot allocate array of void type")
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

object ANY : IType {

    private var referenceType: IReferenceType? = null
    private var arrayType: IArrayType? = null

    override fun reference(): IReferenceType {
        if (referenceType == null)
            referenceType = ReferenceType(this)
        return referenceType!!
    }

    override fun array(): IArrayType {
        if (arrayType == null)
            arrayType = ArrayType(this)
        return arrayType!!
    }

    override fun toString(): String {
        return "Object"
    }

    override fun setProperty(key: String, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getProperty(key: String): Any? {
        TODO("Not yet implemented")
    }

    override var id: String? = "Object"
        set(value) = check(false)

    override val defaultExpression: IExpression
        get() = NULL_LITERAL

    override fun cloneProperties(e: IProgramElement) {
        throw UnsupportedOperationException()
    }

    override val bytes: Int
        get() = 0
}

//val ANY = UnboundType()

class UnboundType(override val defaultExpression: IExpression = NULL_LITERAL) : IType {

    override fun reference(): IReferenceType {
        throw UnsupportedOperationException("cannot get reference to unbound type")
    }

    override fun array(): IArrayType {
        throw UnsupportedOperationException("cannot allocate array of unbound type")
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


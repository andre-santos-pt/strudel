package pt.iscte.strudel.javaparser

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.NULL_LITERAL

class JavaType(val type: Class<*>) : IType {
    override fun reference(): IReferenceType {
        return this.reference()
    }

    override val defaultExpression: IExpression
        get() = NULL_LITERAL

    override fun setProperty(key: String, value: Any?) {
        throw UnsupportedOperationException()
    }

    override fun getProperty(key: String): Any {
        throw UnsupportedOperationException()
    }

    override fun cloneProperties(e: IProgramElement) {
        throw UnsupportedOperationException()
    }

    override var id: String?
        get() = type.name
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun toString(): String = type.name

    override fun isSame(e: IProgramElement): Boolean {
        return e is JavaType && e.type === type
    }

    override val bytes: Int
        get() = TODO("Not yet implemented")
}
package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.Literal
import pt.iscte.strudel.model.impl.ProgramElement


sealed interface IValueType<T> : IType {
    fun matchesLiteral(literal: String): Boolean

    fun literal(t: T): ILiteral {
        require(matchesLiteral(t.toString()))
        return Literal(this, t.toString())
    }

    override fun reference(): IReferenceType {
        throw UnsupportedOperationException()
    }
}

internal abstract class ValueType<T> : ProgramElement(), IValueType<T> {
    override fun toString(): String = id!!

}


val INT: IValueType<Int> = object : ValueType<Int>() {
    override fun matchesLiteral(literal: String): Boolean {
        return literal.matches(Regex("[0-9]+"))
    }

    override val defaultExpression: IExpression
        get() = Literal(this, "0")

    override var id: String?
        get() = "int"
        set(value) {}
}

val DOUBLE: IValueType<Double> = object : ValueType<Double>() {
    override fun matchesLiteral(literal: String): Boolean {
        return literal.matches(Regex("[0-9]+\\.[0-9]+"))
    }

    override val defaultExpression: IExpression
        get() = Literal(this, "0.0")

    override var id: String?
        get() = "double"
        set(value) {}
}

val BOOLEAN: IValueType<Boolean> = object : ValueType<Boolean>() {
    override fun matchesLiteral(literal: String): Boolean {
        return literal.matches(Regex("true|false"))
    }

    override val defaultExpression: IExpression
        get() = Literal(this, "false")

    override var id: String?
        get() = "boolean"
        set(value) {}
}

val CHAR: IValueType<Char> = object : ValueType<Char>() {
    override fun matchesLiteral(literal: String): Boolean {
        return literal.length == 1
    }

    override val defaultExpression: IExpression
        get() = Literal(this, "a")

    override var id: String?
        get() = "char"
        set(value) {}
}


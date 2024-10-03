package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IArrayAccess
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProgramElement

enum class RuntimeErrorType {
    LOOP_MAX,
    STACK_OVERFLOW,
    OUT_OF_MEMORY,

    DIVBYZERO,
    NONINIT_VARIABLE,
    NULL_POINTER,

    ARRAY_INDEX_BOUNDS,
    NEGATIVE_ARRAY_SIZE,

    VALUE_OVERFLOW,

    FOREIGN_PROCEDURE,
    EXCEPTION,
    UNSUPPORTED
}

open class RuntimeError(
    val type: RuntimeErrorType,
    val sourceElement: IProgramElement?,
    override val message: String?) : RuntimeException() {

    override fun toString(): String {
        return "$type at $sourceElement: $message"
    }
}

class ExceptionError(sourceElement: IProgramElement, msg: String?)
    : RuntimeError(RuntimeErrorType.EXCEPTION, sourceElement, msg)

class ArrayIndexError(
    element: IArrayAccess,
    val invalidIndex: Int,
    val indexExpression: IExpression,
    val array: IArray
) : RuntimeError(
    RuntimeErrorType.ARRAY_INDEX_BOUNDS, element.index, "invalid array index access"
) {
    var target: IExpression

    init {
        target = element.target
        //for (i in 0 until indexDimension) target = target.element(element.indexes[i])
    }

    override fun toString(): String {
        return super.toString() +
                " ; invalid index: " + invalidIndex + " ; target: " + target +
                " ; expression: " + indexExpression
    }
}

class NegativeArraySizeError(
    lengthExpression: IExpression,
    val invalidSize: Int
) : RuntimeError(
    RuntimeErrorType.NEGATIVE_ARRAY_SIZE, lengthExpression, "negative array size: $invalidSize"
) {

    override fun toString(): String {
        return super.toString() +
                " ; invalid size: " + invalidSize + " ; expression: " + sourceElement
    }
}

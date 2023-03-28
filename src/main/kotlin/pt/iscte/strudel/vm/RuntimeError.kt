package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IArrayAccess
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProgramElement

enum class RuntimeErrorType {
    NULL_POINTER, ARRAY_INDEX_BOUNDS, NEGATIVE_ARRAY_SIZE, NONINIT_VARIABLE, INFINTE_CYCLE, STACK_OVERFLOW, OUT_OF_MEMORY, VALUE_OVERFLOW, ASSERTION, BUILT_IN_PROCEDURE
}

open class RuntimeError(
    val type: RuntimeErrorType,
    val sourceElement: IProgramElement?,
    override val message: String) : RuntimeException() {

    override fun toString(): String {
        return "$type at $sourceElement: $message"
    }
}


class ArrayIndexError(
    element: IArrayAccess,
    val invalidIndex: Int,
    val indexExpression: IExpression
) : RuntimeError(
    RuntimeErrorType.ARRAY_INDEX_BOUNDS, element, "invalid array index access"
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
    element: IArrayAllocation,
    val invalidSize: Int,
    val sizeExpression: IExpression
) : RuntimeError(
    RuntimeErrorType.NEGATIVE_ARRAY_SIZE, element, "negative array size: $invalidSize"
) {

    override fun toString(): String {
        return super.toString() +
                " ; invalid size: " + invalidSize + " ; expression: " + sizeExpression
    }
}

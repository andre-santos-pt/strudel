package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IArrayAccess
import pt.iscte.strudel.model.IBinaryExpression
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IStatement
import pt.iscte.strudel.model.ITargetExpression
import pt.iscte.strudel.model.IVariableExpression

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
    message: String?
) : RuntimeException(message) {

    override fun toString(): String {
        return "$type at $sourceElement: $message"
    }
}

class StackOverflowError(
    val instruction: IProgramElement?
) : RuntimeError(RuntimeErrorType.STACK_OVERFLOW, instruction, "Stack overflow")

class LoopIterationLimitError(
    val loop: ILoop,
    val limit: Int
) : RuntimeError(RuntimeErrorType.LOOP_MAX, loop, "Loop reached the maximum number of iterations ($limit)")

class NullReferenceError(
    val target: ITargetExpression
) : RuntimeError(RuntimeErrorType.NULL_POINTER, target, "Reference is null: $target")

class DivisionByZeroError(
    val exp: IBinaryExpression
) : RuntimeError(RuntimeErrorType.DIVBYZERO, exp.rightOperand, "Cannot divide by zero")

class UninitializedVariableError(
    val exp: IVariableExpression
) : RuntimeError(RuntimeErrorType.NONINIT_VARIABLE, exp, "Variable not initialised: $exp")

class ArrayIndexError(
    val element: IArrayAccess,
    val invalidIndex: Int,
    val indexExpression: IExpression,
    val array: IArray
) : RuntimeError(
    RuntimeErrorType.ARRAY_INDEX_BOUNDS, element.index, "Invalid array index access: $element"
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
    val lengthExpression: IExpression,
    val invalidSize: Int
) : RuntimeError(
    RuntimeErrorType.NEGATIVE_ARRAY_SIZE, lengthExpression, "Negative array size: $invalidSize"
) {

    override fun toString(): String {
        return super.toString() +
                " ; invalid size: " + invalidSize + " ; expression: " + sourceElement
    }
}

class OutOfMemoryError(
    val objects: MutableList<IValue>,
    val culprit: IValue,
    val source: IProgramElement? = null
): RuntimeError(RuntimeErrorType.OUT_OF_MEMORY, source, "Out of memory: cannot allocate $culprit at $source")

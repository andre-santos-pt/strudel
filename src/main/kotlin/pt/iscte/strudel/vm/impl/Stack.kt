package pt.iscte.strudel.vm.impl


internal class Stack<E> {
    private val stack = mutableListOf<E>()
    private var next = 0

    fun peek(): E {
        check(!isEmpty) { "empty stack" }
        return stack[next - 1]
    }

    fun peekAhead(): E {
        check(stack.size > next) { "no element ahead" }
        return stack[next - 1]
    }

    fun pop(): E {
        check(!isEmpty) { "empty stack" }
        return stack.get(--next)
    }

    fun push(e: E) {
        stack.add(next++, e)
    }

    val isEmpty: Boolean
        get() = next == 0

    fun size() = next

    fun toList() = stack.subList(0, next)

    override fun toString() = stack.toString()

}
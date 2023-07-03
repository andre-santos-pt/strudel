package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.vm.*

internal class CallStack(override val virtualMachine: VirtualMachine, override val maxSize: Int) : ICallStack {
    private val stack: Stack<IStackFrame> = Stack()
    override val size: Int get() = stack.size()

    override val frames: List<IStackFrame>
        get() = stack.toList()

    override val topFrame: IStackFrame
        get() {
            if (size == 0) throw RuntimeException("empty call stack")
            return stack.peek()
        }

    override val previousFrame: IStackFrame?
        get() = stack.peekPrevious()

    override val lastTerminatedFrame: IStackFrame?
        get() = stack.peekAhead()

    override val isEmpty: Boolean
        get() = stack.isEmpty

    override fun toString() = stack.toList().joinToString( " -> ") {it.procedure.toString()}

   // @Throws(RuntimeError::class)
    override fun newFrame(creator: () -> IStackFrame) {
        val frame = creator()
        require(frame.callStack === this)
        if (size == maxSize) throw RuntimeError(RuntimeErrorType.STACK_OVERFLOW, frame.procedure, "stack overflow")
        stack.push(frame)
    }


    override fun terminateTopFrame() {
       // topFrame.returnValue = returnValue
        stack.pop()
    }

}
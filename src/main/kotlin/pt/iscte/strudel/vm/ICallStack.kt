package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.UnboundType
import pt.iscte.strudel.vm.impl.StackFrame

interface ICallStack {
    val maxSize: Int
    val size: Int
    val virtualMachine: IVirtualMachine
    val frames: List<IStackFrame>
    val topFrame: IStackFrame
    val previousFrame: IStackFrame?

    val lastTerminatedFrame: IStackFrame?
    val isEmpty: Boolean

    val memory: Int
        get() {
            var m = 0
            frames.forEach { frame ->
                m += STACK_FRAME_OVERHEAD
                frame.variables.values.forEach { localVar ->
                    m += if (localVar.isNull) 4 else localVar.memory // Null Pointer is still a pointer
                }
            }
            return m
        }

    //@Throws(RuntimeError::class)
    fun newFrame(procedure: IProcedure, args: List<IValue>): IStackFrame {
        val frame =  StackFrame(this, procedure, args)
        newFrame { frame }
        return frame
    }

    //@Throws(RuntimeError::class)
    fun newFrame(creator: () -> IStackFrame)

    fun terminateTopFrame()


}


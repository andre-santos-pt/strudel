package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IProcedure
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
//    val memory: Int
//        get() {
//            var bytes = 0
//            for (f in frames) {
//                bytes += f.memory
//            }
//            return bytes
//        }

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


package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*

const val STACK_FRAME_OVERHEAD = 16

interface IStackFrame {
    val callStack: ICallStack
    val procedure: IProcedure
    val arguments: List<IValue>
    val variables: Map<IVariableDeclaration<*>, IValue>
    var returnValue : IValue?

    val isTopFrame: Boolean
        get() = this === callStack.topFrame

    fun getValue(varId: String): IValue

    operator fun get(it: IVariableDeclaration<*>): IValue?
    operator fun set(target: IVariableDeclaration<*>, value: IValue)
}
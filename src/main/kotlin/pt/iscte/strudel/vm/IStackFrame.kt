package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IRecordType
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.IVariableDeclaration


interface IStackFrame {
    val callStack: ICallStack
    val procedure: IProcedure
    val arguments: List<IValue>
    val variables: MutableMap<IVariableDeclaration<*>, IValue>
    var returnValue : IValue?

    val isTopFrame: Boolean
        get() = this === callStack.topFrame

    fun getValue(varId: String): IValue
}
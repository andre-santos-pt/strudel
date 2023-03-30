package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.*

internal class StackFrame(callStack: ICallStack, procedure: IProcedure, override val arguments: List<IValue>) :
    IStackFrame {
    override val callStack: ICallStack
    override val procedure: IProcedure
    override val variables: MutableMap<IVariableDeclaration<*>, IValue>
    override var returnValue: IValue? = null


    init {
        require(procedure.parameters.size == arguments.size) { "number of arguments do not match (${procedure.id})"}
        procedure.parameters.forEachIndexed {
            i, p ->
           require(p.type.isSame(arguments[i].type))
        }
        this.callStack = callStack
        this.procedure = procedure
        variables = LinkedHashMap()
        returnValue = null
        procedure.parameters.forEachIndexed {
            i, p ->
            variables[p] = arguments[i].copy()
        }
        for (v in procedure.localVariables) {
            variables[v] = NULL
        }
    }

    override fun toString(): String {
        var text = procedure.id + "(...)" // TODO pretty print
        for ((key, value) in variables) text += " $key=$value"
        return text
    }

    override fun getValue(varId: String): IValue {
        val v = variables.toList().find { it.first.id == varId }
        check(v != null)
        return v.second
    }
}
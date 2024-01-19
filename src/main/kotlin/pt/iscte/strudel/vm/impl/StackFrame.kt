package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.*

internal class StackFrame(callStack: ICallStack, procedure: IProcedure, override val arguments: List<IValue>) :
    IStackFrame {
    override val callStack: ICallStack
    override val procedure: IProcedure

    private val variableMap: MutableMap<IVariableDeclaration<*>, IValue>

    override val variables: Map<IVariableDeclaration<*>, IValue>
        get() = variableMap

    override var returnValue: IValue? = null

    init {
        require(procedure.parameters.size == arguments.size) { "number of arguments do not match (${procedure.id})"}
        procedure.parameters.forEachIndexed {
            i, p ->
           require(p.type.isSame(arguments[i].type) || p.type.isReference && arguments[i] == NULL) {
               "Argument types do not match procedure parameter types: ${procedure.id}" +
                       "\n\tExpected: ${procedure.parameters.joinToString { it.type.id ?: "null" }}" +
                       "\n\tBut was: ${arguments.joinToString { it.type.id ?: "null" }}"
           }
        }
        this.callStack = callStack
        this.procedure = procedure
        variableMap = LinkedHashMap()
        returnValue = null
        procedure.parameters.forEachIndexed {
            i, p ->
            variableMap[p] = arguments[i].copy()
        }
        for (v in procedure.localVariables) {
            variableMap[v] = NULL
        }
    }

    override fun get(v: IVariableDeclaration<IBlock>): IValue? = variableMap[v]

    override fun set(target: IVariableDeclaration<*>, value: IValue) {
        val old = variables[target]
        variableMap[target] = value
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
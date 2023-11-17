package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.impl.ProcedureExecution
import pt.iscte.strudel.vm.impl.Reference
import pt.iscte.strudel.vm.impl.VirtualMachine
import pt.iscte.strudel.vm.impl.defaultValue


// TODO GC
interface IVirtualMachine {
    val callStack: ICallStack
    val heapMemory: IHeapMemory
    val callStackMaximum: Int
    val loopIterationMaximum: Int
    val availableMemory: Int
    val usedMemory: Int
    val throwExceptions: Boolean

    val topFrame: IStackFrame get() = callStack.topFrame

    val instructionPointer: IProgramElement?

    val listeners: Set<IListener>

    fun getValue(literal: String): IValue

    fun getValue(any: Any?): IValue =
        if (any == null)
            NULL
        else
            when (any) {
                is Int -> getValue(any)
                is Double -> getValue(any)
                is Boolean -> getValue(any)
                is Char -> getValue(any)
                else -> throw UnsupportedOperationException()
            }

    fun getValue(number: Int): IValue

    fun getValue(number: Double): IValue

    fun getValue(bool: Boolean): IValue

    fun getValue(character: Char): IValue

    fun addListener(l: IListener)

    fun removeListener(l: IListener)

    fun removeAllListeners()

    fun systemOutput(text: String)

    fun allocateArray(baseType: IType, length: Int): IReference<IArray> {
        val array = heapMemory.allocateArray(baseType, length)
        for (i in 0 until array.length)
            array.setElement(i, baseType.defaultValue)

        val ref = Reference(array)
        listeners.forEach {
            it.arrayAllocated(ref)
        }
        return ref
    }

    fun execute(procedure: IProcedure, vararg arguments: IValue): IValue?

    fun allocateArrayOf(baseType: IType, vararg values: Any): IReference<IArray> {
        val a = allocateArray(baseType, values.size)
        values.forEachIndexed { i, e ->

            val v = if (baseType.isValueType)
                getValue(e)
            else if (baseType.isArrayReference) {
                e as IReference<IArray>
//                val array = (e as IReference).target as IArray
//
//                val ref = allocateArray(((baseType as IReferenceType).target as IArrayType).componentType, array.size)
//                for (j in array.indices)
//                    (ref.target as IArray).setElement(j, getValue(array[j]))
//                ref
            } else
                allocateRecord(baseType as IRecordType)

            require(baseType.isSame(v.type))
            (a.target as IArray).setElement(i, v)
        }
        return a
    }

    fun allocateRecord(type: IRecordType): IReference<IRecord> {
        val ref = Reference(heapMemory.allocateRecord(type))
        listeners.forEach { it.recordAllocated(ref) }
        return ref
    }

    var error: RuntimeError?

    val errorOccurred get() = error != null

    fun debug(procedure: IProcedure, vararg arguments: IValue) : ProcedureExecution {
        require(arguments.size == procedure.parameters.size) {
            "number of arguments (${arguments.size}) do not match ${procedure.id}(${procedure.parameters.size})"
        }

        val ex = ProcedureExecution(this, procedure, *arguments)
        ex.init()
        return ex
    }


    interface IListener {
        fun procedureCall(procedure: IProcedure, args: List<IValue>, caller: IProcedure?) { }
        fun procedureEnd(procedure: IProcedure, args: List<IValue>, result: IValue?) { }
        fun returnCall(s: IReturn, returnValue: IValue?) { }
        fun variableAssignment(a: IVariableAssignment, value: IValue) { }
        //fun arrayElementAssignment(a: IArrayElementAssignment, index: Int, value: IValue) { }
        fun arrayElementAssignment(a: IArrayElementAssignment, ref: IReference<IArray>, index: Int, value: IValue) { }
        fun loopIteration(loop: ILoop) { }
        fun arrayAllocated(ref: IReference<IArray>) { }
        fun recordAllocated(ref: IReference<IRecord>) { }
        fun fieldAssignment(a: IRecordFieldAssignment, value: IValue) { }
        fun executionError(e: RuntimeError) { }
        fun systemOutput(text: String) { }
    }

    companion object {
        fun create(
            callStackMaximum: Int = 1024,
            loopIterationMaximum: Int = 1000000,
            availableMemory: Int = 1024,
            throwExceptions: Boolean = true
        ): IVirtualMachine =
            VirtualMachine(callStackMaximum, loopIterationMaximum, availableMemory, throwExceptions)
    }
}


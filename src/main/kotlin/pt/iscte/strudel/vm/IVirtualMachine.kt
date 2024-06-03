package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*
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
                is Long -> getValue(any.toInt()) // TODO: not cool
                is Double -> getValue(any)
                is Float -> getValue(any.toDouble()) // also not that cool
                is Boolean -> getValue(any)
                is Char -> getValue(any)
                else -> throw UnsupportedOperationException("Unsupported Strudel value type ${any::class.simpleName} in: $any")
            }

    fun getValue(number: Int): IValue

    fun getValue(number: Double): IValue

    fun getValue(bool: Boolean): IValue

    fun getValue(character: Char): IValue

    fun getNullReference(): IReference<*>

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
        val array = heapMemory.allocateArray(baseType, values.size)
        values.forEachIndexed { i, e ->
            val v =
                if (baseType.isValueType) getValue(e)
                else if (baseType.isArrayReference) e as IReference<IArray>
                else if (baseType.isRecordReference) e as IReference<IRecord>
                else if (e is IValue) e // Can leave as IValue since require() checks types after anyway
                else allocateRecord(baseType as IRecordType)

            // || (v.type.isReference && baseType.isSame((v.type as IReferenceType).target))
            require(baseType.isSame(v.type) || (v.type.isReference && baseType.isSame((v.type as IReferenceType).target))) {
                "Cannot add element $e of IType ${v.type::class.simpleName} ${v.type.id} to array with base IType ${baseType::class.simpleName} ${baseType.id}"
            }
            // to avoid listener notification
            (array as pt.iscte.strudel.vm.impl.Array).elements[i] = v
        }
        val ref = Reference(array)
        listeners.forEach {
            it.arrayAllocated(ref)
        }
        return ref
    }

    fun allocateRecord(type: IRecordType): IReference<IRecord> {
        val ref = Reference(heapMemory.allocateRecord(type))
        listeners.forEach { it.recordAllocated(ref) }
        return ref
    }

    var error: RuntimeError?

    val errorOccurred get() = error != null

    fun setDefaultSystemOut() {
        addListener(object : IListener {
            override fun systemOutput(text: String) {
                println(text)
            }
        })
    }

    interface IListener {
        fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {}
        fun procedureEnd(procedure: IProcedureDeclaration, args: List<IValue>, result: IValue?) {}
        fun returnCall(s: IReturn, returnValue: IValue?) {}
        fun variableAssignment(a: IVariableAssignment, value: IValue) {}
        fun arrayElementAssignment(a: IArrayElementAssignment, ref: IReference<IArray>, index: Int, value: IValue) {}
        fun loopIteration(loop: ILoop) {}
        fun loopEnd(loop: ILoop) {}
        fun arrayAllocated(ref: IReference<IArray>) {}
        fun recordAllocated(ref: IReference<IRecord>) {}
        fun fieldAssignment(a: IRecordFieldAssignment, ref: IReference<IRecord>, value: IValue) {}
        fun expressionEvaluation(
            e: IExpression,
            context: IExpressionHolder,
            value: IValue,
            concreteExpression: IExpression
        ) {
        }

        fun executionError(e: RuntimeError) {}
        fun systemOutput(text: String) {}
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


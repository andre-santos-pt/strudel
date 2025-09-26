package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.parsing.java.OUTER_PARAM
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.*

internal class VirtualMachine(
    override val callStackMaximum: Int = 512,
    override val loopIterationMaximum: Int = 1000,
    override val availableMemory: Int = 1024,
    override val throwExceptions: Boolean = true
) : IVirtualMachine {
    private val stack: ICallStack
    override val heapMemory: IHeapMemory = Memory()

    override val listeners: Set<IVirtualMachine.IListener> = mutableSetOf()

    init {
        require(callStackMaximum >= 1)
        require(loopIterationMaximum >= 1)
        require(availableMemory >= 1)
        stack = CallStack(this, callStackMaximum)
    }

    override fun addListener(l: IVirtualMachine.IListener) {
        (listeners as MutableSet<IVirtualMachine.IListener>).add(l)
    }

    override fun removeListener(l: IVirtualMachine.IListener) {
        (listeners as MutableSet<IVirtualMachine.IListener>).remove(l)
    }

    override fun removeAllListeners() {
        (listeners as MutableSet<IVirtualMachine.IListener>).clear()
    }

    override val callStack: ICallStack
        get() = stack

    override val usedMemory: Int
        get() = stack.memory + heapMemory.memory

    var call: ProcedureInterpreterNoExpression? = null

    override val instructionPointer: IProgramElement?
        get() = call?.instructionPointer

    private val VALUE_TYPES = listOf<IValueType<*>>(INT, DOUBLE, BOOLEAN, CHAR)

    override fun getValue(literal: String): IValue {
        if (literal == "null") return NULL
        for (type in VALUE_TYPES) {
            if (type.matchesLiteral(literal))
                return Value(
                    type, when (type) {
                        INT -> literal.toInt()
                        DOUBLE -> literal.toDouble()
                        BOOLEAN -> literal.toBoolean()
                        CHAR -> literal[0]
                        else -> null
                    }
                )
        }
        check(false)
        return null!!
    }

    override fun getValue(number: Int): IValue = Value(INT, number)
    override fun getValue(number: Double): IValue = Value(DOUBLE, number)
    override fun getValue(bool: Boolean): IValue = Value(BOOLEAN, bool)
    override fun getValue(character: Char): IValue = Value(CHAR, character)


    override fun getNullReference(): IReference<*> = Reference(NULL)

    //@JsName("execute")
    override fun execute(
        procedure: IProcedureDeclaration,
        vararg arguments: IValue
    ): IValue? {
        require(arguments.size == procedure.parameters.size) {
            "number of arguments (${arguments.size}) do not match ${procedure.id}(${procedure.parameters.size})"
        }
        error = null

        if (procedure is IProcedure)
            return try {
                call = ProcedureInterpreterNoExpression(
                    this,
                    procedure,
                    *arguments
                )
                call?.run()
                call?.returnValue
            } catch (e: RuntimeError) {
                error = e
                listeners.forEach { it.executionError(e) }
                if (throwExceptions)
                    throw e
                else
                    null
            }
        else if(procedure is ForeignProcedure)
            return procedure.run(this, arguments.toList())
        else
            throw RuntimeException("could not execute: ${procedure.id}")
    }

    override fun systemOutput(text: String) {
        listeners.forEach {
            it.systemOutput(text)
        }
    }

    override var error: RuntimeError? = null


    inner class Memory : IHeapMemory {
        private val objects: MutableList<IValue> = mutableListOf()

        private fun add(v: IValue) {
            if (memory + v.memory > availableMemory)
                throw OutOfMemoryError(objects, v, this@VirtualMachine.call?.instructionPointer)
            objects.add(v)
        }

        override fun allocateArray(
            baseType: IType,
            vararg dimensions: Int
        ): IArray {
            require(dimensions.isNotEmpty() && dimensions[0] >= 0)
            var arrayType = baseType.array()
            for (i in 1 until dimensions.size) arrayType = arrayType.array()
            val array = Array(arrayType, dimensions[0])
            if (dimensions.size == 1) {
                for (i in 0 until dimensions[0]) {
                    val v = baseType.defaultValue
                    array.setElement(i, v)
                }
            }
            for (i in 1 until dimensions.size) {
                val remainingDims = dimensions.copyOfRange(i, dimensions.size)
                if (remainingDims[0] != -1)
                    for (j in 0 until dimensions[0])
                        array.setElement(
                            j, allocateArray(baseType, *remainingDims)
                        )
            }
            add(array)
            return array
        }

        override fun allocateRecord(type: IRecordType): IRecord {
            val rec = Record(type)
            add(rec)
            return rec
        }

        override val memory: Int
            get() = objects.sumOf { it.memory }
    }

    inner class Record(override val type: IRecordType) : IRecord {
        private val fields: MutableMap<IVariableDeclaration<IRecordType>, IReference<*>>

        private val listeners = mutableListOf<IRecord.IListener>()

        init {
            fields = LinkedHashMap()
            for (f in type.fields) {
                val v = f.type.defaultValue
                fields[f] = Reference(v)
            }
        }

        override val memory: Int
            get() = type.bytes

        override fun getField(field: IVariableDeclaration<IRecordType>): IValue {
            if (field.type is IReferenceType)
                return if (fields[field]!!.isNull)
                    NULL
                else
                    fields[field]!!
            else
                return fields[field]!!.target
        }

        override fun setField(
            field: IVariableDeclaration<IRecordType>,
            value: IValue
        ) {
            require(fields.containsKey(field)) { field }
            val old = if (value is IReference<*>)
                fields[field]!!
            else
                fields[field]!!.target

            val new = if (value is IReference<*>)
                value.target
            else value

            fields[field] = Reference(new)
            listeners.forEach {
                it.fieldChanged(field, old, new)
            }
        }

        override fun addListener(listener: IRecord.IListener) {
            listeners.add(listener)
        }

        override fun removeListener(listener: IRecord.IListener) {
            listeners.remove(listener)
        }

        override fun toString(): String {
            return "${type.id}(" + fields.filterNot { it.key.id == OUTER_PARAM }.entries.joinToString { it.key.id + ": " + it.value } + ")"
        }


        override val value: Map<IVariableDeclaration<IRecordType>, IValue>
            get() = fields

        override fun copy(): IRecord {
            val record = Record(type)
            for (f in type.fields)
                record.fields[f] = fields[f]!!.copy()
            return record
        }

        override val isNull: Boolean = false
        override val isTrue: Boolean = false
        override val isFalse: Boolean = false
        override val isNumber: Boolean = false
    }
}

val IType.defaultValue: IValue
    get() =
        when (this) {
            INT -> Value(this, 0)
            DOUBLE -> Value(this, 0.0)
            CHAR -> Value(this, ' ')
            BOOLEAN -> Value(this, false)
            else -> NULL
        }




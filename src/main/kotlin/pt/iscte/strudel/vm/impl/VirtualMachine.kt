package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.*

internal open class VirtualMachine(
    override val callStackMaximum: Int = 1024,
    override val loopIterationMaximum: Int = 1000, // TODO
    override val availableMemory: Int = 1000 // TODO,
) : IVirtualMachine {
    private val stack: ICallStack
    override val heapMemory: IHeapMemory

    override val listeners = mutableListOf<IVirtualMachine.IListener>()

    init {
        require(callStackMaximum >= 1)
        require(loopIterationMaximum >= 1)
        require(availableMemory >= 1)
        stack = CallStack(this, callStackMaximum)
        heapMemory = Memory()
    }

    override fun addListener(l: IVirtualMachine.IListener) {
        listeners.add(l)
    }

    override val callStack: ICallStack
        get() = stack

    override val usedMemory: Int
        get() = 0 //stack.memory + heapMemory.memory


    var call: ProcedureExecution? = null

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

    //@JsName("execute")
    override fun execute(
        procedure: IProcedure,
        vararg arguments: IValue
    ): IValue? {
        require(arguments.size == procedure.parameters.size) {
            "number of arguments (${arguments.size}) do not match ${procedure.id}(${procedure.parameters.size})"
        }
        return try {
            call = ProcedureExecution(this, procedure, *arguments)
            call?.run()
            call?.returnValue
        } catch (e: RuntimeError) {
            error = e
            listeners.forEach { it.executionError(e) }
            null
        }
    }

    override var error: RuntimeError? = null


    inner class Memory : IHeapMemory {
        private val objects: MutableList<IValue> = mutableListOf()

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
            objects.add(array)
            return array
        }

        override fun allocateRecord(type: IRecordType): IRecord {
            val rec = Record(type)
            objects.add(rec)
            return rec
        }

        override val memory: Int
            get() = 0
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
            var text = ""
            for ((key, value1) in fields) {
                text += """$key = $value1\n"""
            }
            return if (text.isEmpty()) type.id.toString() else text
        }


        override val value: Map<IVariableDeclaration<IRecordType>, IValue>
            get() = fields

        override fun copy(): IRecord {
            val record = Record(type)
            for (f in type.fields) record.fields[f] =
                getField(f).copy() as IReference<*>
            return record
        }

        override val isNull: Boolean = false
        override val isTrue: Boolean = false
        override val isFalse: Boolean = false
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




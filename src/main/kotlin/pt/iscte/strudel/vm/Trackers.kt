package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.impl.putMulti
import kotlin.math.sign

interface IRecursiveCallCounter {
    operator fun get(procedure: IProcedureDeclaration): Int
}

fun IVirtualMachine.addRecursiveCallCounter(): IRecursiveCallCounter {
    val map = mutableMapOf<IProcedureDeclaration, Int>()
    val counter = object : IRecursiveCallCounter {
        override fun get(procedure: IProcedureDeclaration): Int = map[procedure] ?: 0
    }
    addListener(object: IVirtualMachine.IListener {
        override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
            if (procedure == caller)
                map[procedure] = (map[procedure] ?: 0) + 1
        }
    })
    return counter
}

interface ILoopCounter {
    val allLoops: Set<ILoop>
    operator fun get(loop: ILoop): Int
}

fun IVirtualMachine.addLoopCounter(): ILoopCounter {
    val map = mutableMapOf<ILoop, Int>()
    val counter = object : ILoopCounter {
        override val allLoops: Set<ILoop> get() = map.keys
        override fun get(loop: ILoop): Int = map[loop] ?: 0
    }
    addListener(object : IVirtualMachine.IListener {
        override fun loopIteration(loop: ILoop) {
            map[loop] = (map[loop] ?: 0) + 1
        }
    } )
    return counter
}


interface IVariableTracker {
    val allVariables : Set<IVariableDeclaration<*>>
    operator fun get(v: IVariableDeclaration<*>): List<IValue>
}

fun IVirtualMachine.addVariableTracker(): IVariableTracker {
    val map = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()
    val tracker = object : IVariableTracker {
        override val allVariables: Set<IVariableDeclaration<*>> get() = map.keys
        override fun get(v: IVariableDeclaration<*>): List<IValue> = map[v] ?: emptyList()
    }
    addListener(object : IVirtualMachine.IListener {
        override fun procedureCall(
            s: IProcedureDeclaration,
            args: List<IValue>,
            caller: IProcedure?
        ) {
            s.parameters.forEachIndexed { i, p ->
                if(!map.containsKey(p)) map[p] = mutableListOf(args[i])
                else (map[p] as MutableList).add(args[i])
            }

        }
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if(!map.containsKey(a.target)) map[a.target] = mutableListOf(value)
            else (map[a.target] as MutableList).add(value)
        }
    } )
    return tracker
}

interface IAllocationTracker {
    operator fun get(type: IType): List<IMemory>
    fun allAllocations(): List<IMemory>
}

fun IVirtualMachine.addAllocationTracker(): IAllocationTracker {
    val map = mutableMapOf<IType, List<IMemory>>()

    val tracker = object : IAllocationTracker {
        override fun get(type: IType): List<IMemory> {
            return map[type] ?: emptyList()
        }

        override fun allAllocations(): List<IMemory> {
            return map.values.flatten()
        }
    }
    addListener(object : IVirtualMachine.IListener {
        override fun arrayAllocated(ref: IReference<IArray>) {
            val key = ref.target.type
            if(!map.containsKey(key)) map[key] = mutableListOf(ref.target as IMemory)
            else (map[key] as MutableList).add(ref.target as IMemory)
        }

        override fun recordAllocated(ref: IReference<IRecord>) {
            val key = ref.target.type
            if(!map.containsKey(key)) map[key] = mutableListOf(ref.target as IMemory)
            else (map[key] as MutableList).add(ref.target as IMemory)
        }
    })
    return tracker
}

interface IArraySwapTracker {
    val totalSwaps: Int
    val arrays: Set<IArray>
    operator fun get(array: IArray): List<Pair<Int,Int>>
}

fun IVirtualMachine.addArraySwapTracker(): IArraySwapTracker {

    val counts = mutableMapOf<IArray, MutableList<Pair<Int,Int>>>()

    val tracker = object : IArraySwapTracker {

        override val totalSwaps: Int
            get() = counts.values.sumOf { it.size }

        override val arrays: Set<IArray>
            get() = counts.keys

        override fun get(array: IArray): List<Pair<Int,Int>> = counts[array] ?: emptyList()
    }

    addListener(object : IVirtualMachine.IListener {
        override fun arrayAllocated(ref: IReference<IArray>) {
            ref.target.addListener(object : IArray.IListener {
                var prevIndex: Int? = null
                var prevOld: IValue? = null

                override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                    if (newValue.value == prevOld?.value) {
                        counts.putMulti(ref.target, Pair(prevIndex!!, index))
                        prevIndex = null
                        prevOld = null
                    } else {
                        prevIndex = index
                        prevOld = oldValue
                    }
                }
            })
        }
    })
    return tracker
}
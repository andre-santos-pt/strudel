package pt.iscte.strudel.vm

import pt.iscte.strudel.model.*

interface ILoopCounter {
    operator fun get(loop: ILoop): Int
}

fun IVirtualMachine.addLoopCounter(): ILoopCounter {
    val map = mutableMapOf<ILoop, Int>()
    val counter = object : ILoopCounter {
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
    operator fun get(v: IVariableDeclaration<*>): List<IValue>
}

fun IVirtualMachine.addVariableTracker(): IVariableTracker {
    val map = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()
    val tracker = object : IVariableTracker {
        override fun get(v: IVariableDeclaration<*>): List<IValue> = map[v] ?: emptyList()
    }
    addListener(object : IVirtualMachine.IListener {
        override fun procedureCall(
            s: IProcedureDeclaration,
            args: List<IValue>,
            caller: IProcedureDeclaration?
        ) {
            s.parameters.forEachIndexed {i, p ->
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
package pt.iscte.strudel.examples

import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

fun variableHistory(
    vm: IVirtualMachine,
    procedure: IProcedure,
    vararg arguments: IValue
): Map<IVariableDeclaration<*>, List<IValue>> {
    val history = mutableMapOf<IVariableDeclaration<*>, MutableList<IValue>>()

    val listener = object : IVirtualMachine.IListener {
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            history.putIfAbsent(a.target, mutableListOf())
            history[a.target]?.add(value)
        }
    }

    vm.addListener(listener)
    vm.execute(procedure, *arguments)
    vm.removeListener(listener)

    return history
}

fun main() {
    val javaCode = """
        public class BinarySearch {   
           public static int search(int[] a, int e) {
                int lo = 0;
                int hi = a.length - 1;
                while (lo <= hi) {
                    int mid = lo + (hi - lo) / 2;
                    int value = a[mid];
                    if (value < e)
                        lo = mid + 1;
                    else if (value > e)
                        hi = mid - 1;
                    else
                        return mid;
                }
                return -1;
           }
        }
    """.trimIndent()
    val search: IProcedure = Java2Strudel().load(javaCode)["search"]

    val vm = IVirtualMachine.create()

    val array = vm.allocateArrayOf(INT, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val target = vm.getValue(3)

    variableHistory(vm, search, array, target).forEach { (variable, probes) ->
        println("${variable.id} = ${probes.joinToString(" | ")}")
    }
}
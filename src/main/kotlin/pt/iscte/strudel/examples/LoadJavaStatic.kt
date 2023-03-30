package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.VirtualMachine

fun main() {
    val javaCode = """
        class ArraySearch {
            static boolean linearSearch(int[] a, int e) {
                for(int i = 0; i < a.length; i++)
                    if(a[i] == e)
                        return true;
                return false;
            }
            static boolean binarySearch(int[] a, int e) {
                int l = 0;
                int r = a.length - 1;
                while (l <= r) {
                    int m = l + ((r - l) / 2);
                    if (a[m] == e) return true;
                    if (a[m] < e) l = m + 1;
                    else r = m - 1;
                }
                return false;
            }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    println(module)

    runSearch(module.getProcedure("linearSearch"))
    runSearch(module.getProcedure("binarySearch"))
}


private fun runSearch(procedure: IProcedure) {
    val vm = VirtualMachine()
    var iterations = 0
    vm.addListener(object : IVirtualMachine.IListener {
        override fun loopIteration(loop: ILoop) {
           iterations++
        }
    })
    val array = vm.allocateArrayOf(INT, 1, 1, 2, 3, 5, 7, 8, 10, 13, 14, 16, 17, 20)
    val numberTrue = vm.getValue(16)
    println(vm.execute(procedure, array, numberTrue))
    println("loop iterations: $iterations")
}
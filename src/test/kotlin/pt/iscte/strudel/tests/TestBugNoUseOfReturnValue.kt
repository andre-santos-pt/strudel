package pt.iscte.strudel.tests

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TestBugNoUseOfReturnValue {

    val code = """
    class Simple {
        static void swap(int[] a, int sourceIndex, int targetIndex) {
            int t = a[sourceIndex]; 
            a[sourceIndex] = a[targetIndex]; 
            a[targetIndex] = t; 
        }

        static int[] reverse(int[] a) {
            int i, k, t;
            int n = a.length;
            for (i = 0; i < n / 2; i++) {
                swap(a, i, n - i - 1);
            }
            return a;
        }

        public static void main() {
            int[] array = {1, 2, 3, 4};
            array = reverse(array);
            int[] array2 = {10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
            reverse(array2);
        }
    }
    """.trimIndent()

    @Test
    fun test() {
        val paddle = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        var count = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                count++
            }
        })
        val m = vm.execute(paddle["main"])
        assertEquals(2, count)
        println(m)
    }
}
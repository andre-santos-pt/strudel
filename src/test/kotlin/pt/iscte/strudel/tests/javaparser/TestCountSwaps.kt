package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.*
import kotlin.test.assertEquals

class TestCountSwaps {

    @Test
    fun testDirectSwap() {
        val src = """
             class Test {
                 static void invertLeftRight() {
                    int[] a = {1, 2, 3, 4, 5};
                    for(int i = 0; i < a.length/2; i++) {
                        int temp = a[i];
                        a[i] = a[a.length - i - 1];
                        a[a.length - i - 1] = temp;
                    }
                }
                
                 static void invertRightLeft() {
                    int[] a = {1, 2, 3, 4, 5};
                    for(int i = 0; i < a.length/2; i++) {
                        int temp = a[a.length - i - 1];
                        a[a.length - i - 1] = a[i];
                        a[i] = temp;
                    }
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()

        val swapTracker = vm.addArraySwapTracker()

        vm.execute(module.getProcedure("invertLeftRight"))
        assertEquals(2, swapTracker.totalSwaps)

        assertEquals(listOf(Pair(0,4), Pair(1, 3)), swapTracker[swapTracker.arrays.first()])

        vm.execute(module.getProcedure("invertRightLeft"))
        assertEquals(4, swapTracker.totalSwaps)

        assertEquals(listOf(Pair(4,0), Pair(3, 1)), swapTracker[swapTracker.arrays.drop(1).first()])
    }

    @Test
    fun testNoSwap() {
        val src = """
             class Test {
                 static void shift() {
                    int[] a = {1, 2, 3, 4, 5};
                    int temp = a[0];
                    for(int i = 1; i < a.length; i++) {
                       a[i - 1] = a[i];
                    }
                    a[a.length - 1] = temp;
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()

        val swapTracker = vm.addArraySwapTracker()

        vm.execute(module.getProcedure("shift"))
        assertEquals(0, swapTracker.totalSwaps)
    }

    @Test
    fun testInvokeSwap() {
        val src = """
            class Test {
                 static void invert() {
                    int[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
                    for(int i = 0; i < a.length/2; i++) {
                        swap(i, a.length - i - 1, a);
                    }
                }
                
                static void swap(int i, int j, int[] a) {
                    int temp = a[i];
                    a[i] = a[j];
                    a[j] = temp;
                }
            }
            
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()

        val swapTracker = vm.addArraySwapTracker()

        vm.execute(module.getProcedure("invert"))
        assertEquals(5, swapTracker.totalSwaps)
    }
}
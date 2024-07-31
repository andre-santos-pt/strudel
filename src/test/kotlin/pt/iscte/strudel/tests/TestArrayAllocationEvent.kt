package pt.iscte.strudel.tests

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class TestArrayAllocationEvent {

    val code = """
    class Simple {
        public static void main() {
            int[] array0 = new int[3];
            //int[] array1 = new int[] {0, 1, 0, 1};  // not supported
            int[] array2 = {1, 2, 3, 4};
            int[] array3 = {5, 6, 7, 8, 9};
            
        }
    }
    """.trimIndent()

    @Test
    fun test() {
        val paddle = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        var count = 0
        val expected = listOf(
            listOf(0,0,0),
            //listOf(0,1,0,1),
            listOf(1,2,3,4),
            listOf(5,6,7,8,9)
        )
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                assertEquals(expected[count++], ref.target.elements.map { it.value })
            }
        })
        val m = vm.execute(paddle["main"])
        assertEquals(expected.size, count)
        println(m)
    }
}
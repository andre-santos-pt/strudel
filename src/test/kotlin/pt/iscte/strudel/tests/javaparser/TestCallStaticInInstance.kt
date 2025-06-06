package pt.iscte.strudel.tests.javaparser

import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TestCallStaticInInstance {

    val src = """
        class Test {
            static int test(int[] arr) {
                Other obj = new Other();
                int i = obj.random();
                int j = Other.random();
                return i + j;
            }
        }

        class Other {
            static int random() {
                return 7;
            }
        }
    """.trimIndent()
    @Test
    fun test() {
        val m = Java2Strudel().load(src, "Test")
        println(m)
        val vm = IVirtualMachine.create()
        val res = vm.execute(m["test"], vm.allocateArrayOf(INT, 1,2,3))
        assertEquals(14, res?.toInt())
    }

}
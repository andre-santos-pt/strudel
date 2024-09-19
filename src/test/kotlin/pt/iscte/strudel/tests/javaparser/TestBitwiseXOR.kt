package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestBitwiseXOR {

    @Test
    fun testInt() {
        val src = """
            class Test {
                static int exclusiveOr(int a, int b) {
                    return a ^ b;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val exclusiveOr = module.getProcedure("exclusiveOr")

        val vm = IVirtualMachine.create()

        assertEquals(1 xor 9, vm.execute(exclusiveOr, vm.getValue(1), vm.getValue(9))?.value)
        assertEquals(2 xor 8, vm.execute(exclusiveOr, vm.getValue(2), vm.getValue(8))?.value)
        assertEquals(3 xor 7, vm.execute(exclusiveOr, vm.getValue(3), vm.getValue(7))?.value)
        assertEquals(4 xor 6, vm.execute(exclusiveOr, vm.getValue(4), vm.getValue(6))?.value)
        assertEquals(5 xor 5, vm.execute(exclusiveOr, vm.getValue(5), vm.getValue(5))?.value)
        assertEquals(6 xor 4, vm.execute(exclusiveOr, vm.getValue(6), vm.getValue(4))?.value)
        assertEquals(7 xor 3, vm.execute(exclusiveOr, vm.getValue(7), vm.getValue(3))?.value)
        assertEquals(8 xor 2, vm.execute(exclusiveOr, vm.getValue(8), vm.getValue(2))?.value)
        assertEquals(9 xor 1, vm.execute(exclusiveOr, vm.getValue(9), vm.getValue(1))?.value)
    }
}
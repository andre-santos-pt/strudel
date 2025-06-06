package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.ReturnError
import pt.iscte.strudel.vm.IVirtualMachine

class TestJavaAssert {

    @Test
    fun test() {
        val src = """
            class Test {
                public static double sqrt(double x) {
                    assert x >= 0;
                    return Math.sqrt(x);
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val vm = IVirtualMachine.create(throwExceptions = false)

        val sqrt = module.getProcedure("sqrt")

        assertDoesNotThrow {
            assertEquals(0.0, vm.execute(sqrt, vm.getValue(0))?.value)
            assertEquals(2.0, vm.execute(sqrt, vm.getValue(4))?.value)
            assertEquals(4.0, vm.execute(sqrt, vm.getValue(16))?.value)
        }
        val r = vm.execute(sqrt, vm.getValue(-1))
        assertNull(r)
        assertNotNull(vm.error)
        assertEquals(ReturnError.ASSERTION_FAILED.toString(), vm.error?.message)
    }

    @Test
    fun testMessage() {
        val src = """
            class Test {
                public static double sqrt(double x) {
                    assert x >= 0 : "must be positive, but found " + x;
                    return Math.sqrt(x);
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val vm = IVirtualMachine.create(throwExceptions = false)

        val sqrt = module.getProcedure("sqrt")

        val r  = vm.execute(sqrt, vm.getValue(-1.5))
        assertNull(r)
        assertNotNull(vm.error)
        assertEquals("must be positive, but found -1.5", vm.error?.message)
    }
}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.ExceptionError
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestJavaAssert {

    @Test
    fun test() {
        val src = """
            public class Test {
                public static double sqrt(double x) {
                    assert x >= 0;
                    return Math.sqrt(x);
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val vm = IVirtualMachine.create()

        val sqrt = module.getProcedure("sqrt")

        assertDoesNotThrow {
            assertEquals(0.0, vm.execute(sqrt, vm.getValue(0))?.value)
            assertEquals(2.0, vm.execute(sqrt, vm.getValue(4))?.value)
            assertEquals(4.0, vm.execute(sqrt, vm.getValue(16))?.value)
        }

        assertThrows<ExceptionError> {
            vm.execute(sqrt, vm.getValue(-1))
            vm.execute(sqrt, vm.getValue(-2))
            vm.execute(sqrt, vm.getValue(-3))
        }
    }
}
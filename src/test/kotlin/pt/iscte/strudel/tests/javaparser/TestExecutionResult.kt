package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.tests.procedure
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import pt.iscte.strudel.vm.ArrayIndexError

class TestExecutionResult {
    val code = """
        class Test {
            static int main() {
                int[] v = {1,2,3};
                int s = 0;
                for(int i = 0; i < v.length; i++) {
                    s += v[i];
                }
                return s;
            }
        }
    """.trimIndent()

    val codeError = """
        class Test {
            static int main() {
                int[] v = {1,2,3};
                int s = 0;
                for(int i = 0; i <= v.length; i++) {
                    s += v[i];
                }
                return s;
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        vm.executionResult(model.procedure("main")).onSuccess {
            assertEquals(it?.toInt(), 6)
        }.onFailure {
           fail("call should not fail")
        }
    }

    @Test
    fun testError() {
        val model = Java2Strudel().load(codeError)
        val vm = IVirtualMachine.create()
        vm.executionResult(model.procedure("main")).onSuccess {
            fail("call should fail")
        }.onFailure {
            assertTrue(it is ArrayIndexError)
        }
    }
}
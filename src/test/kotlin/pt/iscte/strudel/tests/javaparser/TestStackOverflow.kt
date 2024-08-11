package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError

class TestStackOverflow {

    val code = """    
         class Factorial {

    static int f(int n) {
        return f(n-1);
    }
}
    """.trimIndent()
    @Test
    fun testStackOverflow() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        assertThrows<RuntimeError> {
            vm.execute(module.get("f"),vm.getValue(5))
        }
    }
}
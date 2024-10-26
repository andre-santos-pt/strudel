package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestJavaStringIncrement {

    @Test
    fun test() {
        val src = """
            class StrUtil {
                static String ping(String x) {
                    String y = x;
                    y += " pong!";
                    return y;
                }
            }
        """.trimIndent()
        val module = assertDoesNotThrow { Java2Strudel().load(src) }
        println(module)

        val ping = module.getProcedure("ping")

        val vm = IVirtualMachine.create()

        assertEquals("ping pong!", vm.execute(ping, getString("ping"))?.referenceValue)
    }
}
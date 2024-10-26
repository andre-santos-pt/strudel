package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestStringEquals {

    @Test
    fun testEquals() {
        val src = """
            class StringEquals {
                public static boolean areEqual(String x, String y) {
                    return x.equals(y);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()
        val areEqual = module.getProcedure("areEqual")

        val result = vm.execute(areEqual, getString("hello"), getString("hello"))
        assertEquals(true, result?.toBoolean())
    }

}
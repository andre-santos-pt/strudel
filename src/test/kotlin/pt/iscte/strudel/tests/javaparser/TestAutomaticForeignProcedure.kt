package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestAutomaticForeignProcedure {

    @Test
    fun testOneStaticForeignProcedure() {
        val src = """
            public class Rounder {
                public static int round(double x) {
                    return Math.round(x);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val procedure = module.getProcedure("round")
        assertEquals(3, vm.execute(procedure, vm.getValue(3.14))?.value)
    }

    @Test
    fun testMultipleStaticForeignProcedures() {
        val src = """
            public class Rounder {
                public static int round(double x) {
                    return Math.round(x);
                }
                
                public static int integerValueOf(String x) {
                    return Integer.valueOf(x);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()
        val round = module.getProcedure("round")
        val valueOf = module.getProcedure("integerValueOf")

        assertEquals(3, vm.execute(round, vm.getValue(3.14))?.value)

        val str = getString("8")
        assertEquals(8, vm.execute(valueOf, str)?.value)
    }

    @Test
    fun testMultipleInstanceForeignProcedures() {
        val src = """
            public class ExtremelyUsefulStringMethods {
                public static String substr(String x) {
                    return x.substring(1, 8);
                }
                
                public static String substrToEnd(String x) {
                    return x.substring(1);
                }
                
                public static String length(String s) {
                    return s.length();
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()

        val substr = module.getProcedure("substr")
        val substrToEnd = module.getProcedure("substrToEnd")
        val length = module.getProcedure("length")

        val str = getString("hello world")
        assertEquals("ello wo", vm.execute(substr, str)?.value)
        assertEquals("ello world", vm.execute(substrToEnd, str)?.value)

        assertEquals(0, vm.execute(length, getString(""))?.value)
        assertEquals(1, vm.execute(length, getString("a"))?.value)
        assertEquals(3, vm.execute(length, getString("abc"))?.value)
    }
}
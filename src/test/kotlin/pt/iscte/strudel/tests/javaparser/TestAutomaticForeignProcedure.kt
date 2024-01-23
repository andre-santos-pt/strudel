package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestAutomaticForeignProcedure {

    @Test
    fun test1() {
        val src = """
            public class Rounder {
                public static double round(int x) {
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
    fun test2() {
        val src = """
            public class Rounder {
                public static double round(int x) {
                    return Math.round(x);
                }
                
                public static int integerValueOf(String x) {
                    return Integer.valueOf(x);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val round = module.getProcedure("round")
        val valueOf = module.getProcedure("integerValueOf")
        println(valueOf)
        assertEquals(3, vm.execute(round, vm.getValue(3.14))?.value)
        assertEquals(8, vm.execute(valueOf, vm.getValue("\"8\""))?.value)
    }
}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestSameIdentifierVariables {

    @Test
    fun test() {
        val src = """
            public class ExtremelyUsefulStaticMethods {
                public static int get() {
                    int result = 0;
                    
                    for (int i = 0; i < 10; i++) {
                        result += 2;
                    }
                    
                    for (int i = 0; i < 10; i++) {
                        result -= 1;
                    }
                    
                    return result;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")

        println()
        println(get.variables.joinToString("\n"))

        assertEquals(10, vm.execute(get)?.value)
    }
}
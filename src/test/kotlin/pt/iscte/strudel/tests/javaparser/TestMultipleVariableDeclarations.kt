package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestMultipleVariableDeclarations {

    @Test
    fun testOneVariable() {
        val src = """
            class ExtremelyUsefulStaticMethods {
                public static int get() {
                    int i = 10;
                    return i + 10;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")

        assertEquals(20, vm.execute(get)?.value)
    }

    @Test
    fun testTwoVariablesBothInitializer() {
        val src = """
            class ExtremelyUsefulStaticMethods {
                public static int get() {
                    int i = 10, j = 20;
                    return i + j;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")

        assertEquals(30, vm.execute(get)?.value)
    }

    /*
    @Test
    fun testTwoVariablesOnlyOneInitializer() {
        val src = """
            public class ExtremelyUsefulStaticMethods {
                public static int get() {
                    int i, j = 42;
                    return i + j;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")

        // NPE for uninitialized variable usage
        assertThrows<Exception> { vm.execute(get) }
    }
     */
}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestMethodNameOverloading {

    @Test
    fun test() {
        val src = """
            public class Test {
                public static int root(int p) {
                    return p;
                }
                
                public static int root(int q, int p) {
                    return p + q;
                }
            }
        """.trimIndent()
        val module = assertDoesNotThrow { Java2Strudel().load(src) }
        println(module)

        val root1 = assertDoesNotThrow { module.getProcedure("root", INT) }
        val root2 = assertDoesNotThrow { module.getProcedure("root", INT, INT) }

        val vm = IVirtualMachine.create()

        val p = vm.getValue(3)
        val q = vm.getValue(7)

        assertEquals(3, vm.execute(root1, p)?.value)
        assertEquals(7, vm.execute(root1, q)?.value)
        assertEquals(6, vm.execute(root2, p, p)?.value)
        assertEquals(14, vm.execute(root2, q, q)?.value)
        assertEquals(10, vm.execute(root2, p, q)?.value)
        assertEquals(10, vm.execute(root2, q, p)?.value)
    }
}
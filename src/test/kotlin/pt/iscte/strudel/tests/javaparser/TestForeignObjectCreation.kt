package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestForeignObjectCreation {

    val src = """
        import java.util.Random;
        
        public class StdRandom {
            public static int rand() {
                Random random = new Random();
                return random.nextInt(11);
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val module = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()

        val rand = module.getProcedure("rand")
        val result = vm.execute(rand)?.value

        assertIs<Int>(result)
        assertTrue(result in 0..10)
    }
}
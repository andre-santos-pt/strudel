package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertTrue

class TestForeignObjectCreation {

    val src = """
        import java.util.Random;
        
        class StdRandom {
            public static int rand() {
                Random random = new Random();
                return random.nextInt(11);
            }
            
            public static Random newRandom() {
                return new Random();
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val module = Java2Strudel().load(src)
        println(module)
        val vm = IVirtualMachine.create()

        val rand = module.getProcedure("rand")
        val result = vm.execute(rand)?.value

        assertTrue(result is Int)
        assertTrue(result in 0..10)

        val newRandom = module.getProcedure("newRandom")
        val ref = vm.execute(newRandom)?.referenceValue
        assertTrue(ref is java.util.Random)
    }
}
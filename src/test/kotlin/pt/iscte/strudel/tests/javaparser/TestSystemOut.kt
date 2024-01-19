package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.tests.procedure
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestSystemOut {
    val code = """
        class Test {
            static void main() {
                System.out.println("hello");
                System.out.print("!");
                print(" this is ");
                println("Strudel!");
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        var out = mutableListOf<String>()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun systemOutput(text: String) {
               out.add(text)
            }
        })
        vm.execute(model.procedure("main"))
        assertEquals(listOf("hello\n","!"," this is ", "Strudel!\n"), out)
    }
}
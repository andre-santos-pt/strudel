package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestStringCreationArray {

    @Test
    fun test() {
        val src = """
            class Newline {
                public static void main() {
                    char[] array = {'h','e','l','l','o'};
                    String hello = new String(array);
                    System.out.print(hello);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)
        val vm = IVirtualMachine.create()
        val main = module.getProcedure("main")
        vm.addListener(object: IVirtualMachine.IListener {
            override fun systemOutput(text: String) {
                assertEquals("hello", text)
            }
        })
        vm.execute(main)
    }


}
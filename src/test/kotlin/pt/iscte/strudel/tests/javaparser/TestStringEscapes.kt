package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals

class TestStringEscapes {

    @Test
    fun testNewLine() {
        val src = """
            class Newline {
                public static void main() {
                   String s = "hello\nworld";
                   System.out.println(s);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()
        val main = module.getProcedure("main")
        vm.addListener(object: IVirtualMachine.IListener {
            override fun systemOutput(text: String) {
                assertEquals("hello\nworld" + System.lineSeparator(), text)
            }
        })
        vm.execute(main)
    }

    @Test
    fun testQuotes() {
        val src = """
            class Newline {
                public static void main() {
                    String s = "hello \"world\"";
                   System.out.print(s);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()
        val main = module.getProcedure("main")
        vm.addListener(object: IVirtualMachine.IListener {
            override fun systemOutput(text: String) {
                assertEquals("hello \"world\"", text)
            }
        })
        vm.execute(main)
    }

    @Test
    fun testSlash() {
        val src = """
            class Newline {
                public static void main() {
                   String s = "AC\\DC";
                   System.out.print(s);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create()
        val main = module.getProcedure("main")
        vm.addListener(object: IVirtualMachine.IListener {
            override fun systemOutput(text: String) {
                assertEquals("AC\\DC", text)
            }
        })
        vm.execute(main)
    }

}
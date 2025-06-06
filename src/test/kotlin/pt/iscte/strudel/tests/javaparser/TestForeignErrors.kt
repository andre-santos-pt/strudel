package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.sqrt
import org.junit.jupiter.api.Assertions.assertEquals

class TestForeignErrors {

    @Test
    fun test() {
        val src = """
            class Test {
                public static void main() {
                    String s = "hello world!";
                    for(int i = s.length(); i > 0; i--)
                        System.out.println(s.charAt(i));

                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)

        val vm = IVirtualMachine.create(throwExceptions = false)
        val main = module.getProcedure("main")
        vm.execute(main)
        assertEquals("String index out of range: 12", vm.error?.message)
    }

}
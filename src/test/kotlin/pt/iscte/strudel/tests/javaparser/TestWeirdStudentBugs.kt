package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestWeirdStudentBugs {

    // It doesn't matter if their code is correct / produces correct results, I just wanted to check
    // if no weird exceptions were thrown and if the types of expressions were evaluated correctly, etc.

    @Test
    fun testCharIsVowel() {
        val src = """
            class Test {
                static boolean isVowel(char c) {
                    return (int) c == 97 || (int) c == 101 || (int) c == 105 || (int) c == 111 || (int) c == 117 ? true : false;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val isVowel = module.getProcedure("isVowel")

        val vm = IVirtualMachine.create()

        assertTrue(vm.execute(isVowel, vm.getValue('a'))?.value as Boolean)
        assertTrue(vm.execute(isVowel, vm.getValue('e'))?.value as Boolean)
        assertTrue(vm.execute(isVowel, vm.getValue('i'))?.value as Boolean)
        assertTrue(vm.execute(isVowel, vm.getValue('o'))?.value as Boolean)
        assertTrue(vm.execute(isVowel, vm.getValue('u'))?.value as Boolean)

        assertFalse(vm.execute(isVowel, vm.getValue('c'))?.value as Boolean)
        assertFalse(vm.execute(isVowel, vm.getValue('d'))?.value as Boolean)
        assertFalse(vm.execute(isVowel, vm.getValue('g'))?.value as Boolean)
        assertFalse(vm.execute(isVowel, vm.getValue('h'))?.value as Boolean)
        assertFalse(vm.execute(isVowel, vm.getValue('k'))?.value as Boolean)
    }

    @Test
    fun testDoubleToChar() {
        val src = """
            class Test {
                static int round(double n) {
                    char c = (char) n;
                    return (int) c;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val round = module.getProcedure("round")

        val vm = IVirtualMachine.create()

        assertEquals(2, vm.execute(round, vm.getValue(2.1))?.value)
        assertEquals(3, vm.execute(round, vm.getValue(3.2))?.value)
        assertEquals(4, vm.execute(round, vm.getValue(4.2))?.value)
        assertEquals(5, vm.execute(round, vm.getValue(5.4))?.value)
        assertEquals(6, vm.execute(round, vm.getValue(6.3))?.value)
    }
}
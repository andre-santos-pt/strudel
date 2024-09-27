package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertTrue

class TestCharArithmetic {

    @Test
    fun testArithmeticOperators() {
        val src = """
            class CharArithmetic {
                public static char next(char c) {
                    if (c == 'z')
                        return 'a';
                    else
                        return (char) (c + 1);
                }
                
                public static char previous(char c) {
                    if (c == 'a')
                        return 'z';
                    else
                        return (char) (c - 1);
                }
                
                public static int differenceOf(char a, char b) {
                    return a - b;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val next = module.getProcedure("next")
        val previous = module.getProcedure("previous")
        val differenceOf = module.getProcedure("differenceOf")

        val vm = IVirtualMachine.create()

        assertEquals('a', vm.execute(next, vm.getValue('z'))?.value)
        assertEquals('b', vm.execute(next, vm.getValue('a'))?.value)
        assertEquals('c', vm.execute(next, vm.getValue('b'))?.value)
        assertEquals('d', vm.execute(next, vm.getValue('c'))?.value)
        assertEquals('e', vm.execute(next, vm.getValue('d'))?.value)
        assertEquals('f', vm.execute(next, vm.getValue('e'))?.value)

        assertEquals('z', vm.execute(previous, vm.getValue('a'))?.value)
        assertEquals('a', vm.execute(previous, vm.getValue('b'))?.value)
        assertEquals('b', vm.execute(previous, vm.getValue('c'))?.value)
        assertEquals('c', vm.execute(previous, vm.getValue('d'))?.value)
        assertEquals('d', vm.execute(previous, vm.getValue('e'))?.value)
        assertEquals('e', vm.execute(previous, vm.getValue('f'))?.value)

        assertEquals('a' - 'b', vm.execute(differenceOf, vm.getValue('a'), vm.getValue('b'))?.value)
        assertEquals('b' - 'c', vm.execute(differenceOf, vm.getValue('b'), vm.getValue('c'))?.value)
        assertEquals('c' - 'd', vm.execute(differenceOf, vm.getValue('c'), vm.getValue('d'))?.value)
        assertEquals('d' - 'e', vm.execute(differenceOf, vm.getValue('d'), vm.getValue('e'))?.value)
    }

    @Test
    fun testRelationalOperators() {
        val src = """
            class CharRelational {
                public static boolean after(char c1, char c2) {
                    return c1 > c2;
                }
                
                public static boolean before(char c1, char c2) {
                    return c1 < c2;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val after = module.getProcedure("after")
        val before = module.getProcedure("before")

        val vm = IVirtualMachine.create()

        assertTrue(vm.execute(after, vm.getValue('b'), vm.getValue('a'))?.value as Boolean)
        assertTrue(vm.execute(after, vm.getValue('c'), vm.getValue('b'))?.value as Boolean)
        assertTrue(vm.execute(after, vm.getValue('c'), vm.getValue('a'))?.value as Boolean)

        assertTrue(vm.execute(before, vm.getValue('a'), vm.getValue('b'))?.value as Boolean)
        assertTrue(vm.execute(before, vm.getValue('b'), vm.getValue('c'))?.value as Boolean)
        assertTrue(vm.execute(before, vm.getValue('a'), vm.getValue('c'))?.value as Boolean)
    }
}
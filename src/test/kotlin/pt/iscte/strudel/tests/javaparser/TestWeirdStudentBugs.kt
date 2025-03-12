package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.model.CHAR
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
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

    @Test
    fun testIntBitwiseXor() {
        val src = """
            class Test {
                static int square(int n) {
                    return n^2;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val square = module.getProcedure("square")

        val vm = IVirtualMachine.create()

        assertDoesNotThrow {
            vm.execute(square, vm.getValue(0))
            vm.execute(square, vm.getValue(1))
            vm.execute(square, vm.getValue(2))
            vm.execute(square, vm.getValue(3))
            vm.execute(square, vm.getValue(4))
            vm.execute(square, vm.getValue(5))
        }
    }

    @Test
    fun testCompareCharWithInt() {
        val src = """
            class Test {
                static boolean isVowel(char c) {
                    return 'c' >= 97 && 'c' <= 122;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val isVowel = module.getProcedure("isVowel")

        val vm = IVirtualMachine.create()

        assertDoesNotThrow {
            vm.execute(isVowel, vm.getValue('a'))
            vm.execute(isVowel, vm.getValue('e'))
            vm.execute(isVowel, vm.getValue('i'))
            vm.execute(isVowel, vm.getValue('o'))
            vm.execute(isVowel, vm.getValue('u'))
        }
    }

    @Test
    fun testArraySum() {
        val src = """
            class Test {
                static int foo() {
                    int[] numbers = {1, 2, 3, 4}; 
                    int sum = numbers[0];
                    sum = sum + numbers[1];
                    sum = sum + numbers[2];
                    sum = sum + numbers[3];
                    return sum;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val foo = module.getProcedure("foo")

        val vm = IVirtualMachine.create()

        assertEquals(10, vm.execute(foo)?.value)
    }

    @Test
    fun testCharArray() {
        val src = """
            class CharArrayReplace {
                static void replaceLast(char[] letters, char find, char replace) {
                    int i = letters.length - 1;
                    while (i >= 0) {
                        if (letters[i] == find) {
                            letters[i] = replace;
                            return;
                        }
                        i--;
                    }
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val replaceLast = module.getProcedure("replaceLast")

        val vm = IVirtualMachine.create()

        val letters = vm.allocateArrayOf(CHAR, 'a', 'b', 'c', 'a')
        val find = vm.getValue('a')
        val replace = vm.getValue('d')

        assertEquals('a', letters.target.elements.last().value)

        vm.execute(replaceLast, letters, find, replace)

        assertEquals('d', letters.target.elements.last().value)
    }

    @Test
    fun testArrayWrites() {
        val src = """
            class IntArraySwap {
                static void swap(int[] array, int i, int j) {
                    int save = array[i];
                    array[i] = array[j];
                    array[j] = save;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        println(module)

        val swap = module.getProcedure("swap")

        val vm = IVirtualMachine.create()

        var count = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                println("Allocated array: $ref")
                ref.target.addListener(object : IArray.IListener {
                    override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                        println("Write $index")
                        count++
                    }
                })
            }
        })

        val array = vm.allocateArrayOf(INT, 1, 2, 3, 4)
        val i = vm.getValue(1)
        val j = vm.getValue(3)

        vm.execute(swap, array, i, j)

        assertEquals(2, count)
    }
}
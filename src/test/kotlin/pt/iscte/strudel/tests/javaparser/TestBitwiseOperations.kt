package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals

class TestBitwiseOperations {

    @Test
    fun testXor() {
        val src = """
            class Test {
                static int exclusiveOr(int a, int b) {
                    return a ^ b;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val exclusiveOr = module.getProcedure("exclusiveOr")

        val vm = IVirtualMachine.create()

        (1 .. 10).forEach {
            assertEquals(
                it xor (10 - it),
                vm.execute(exclusiveOr, vm.getValue(it), vm.getValue(10 - it))?.value
            )
        }
    }

    @Test
    fun testAndOr() {
        val src = """
            class Test {
                static int and(int a, int b) {
                    return a & b;
                }
                
                static int or(int a, int b) {
                    return a | b;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val and = module.getProcedure("and")
        val or = module.getProcedure("or")

        val vm = IVirtualMachine.create()

        (1 .. 10).forEach {
            assertEquals(
                it and (10 - it),
                vm.execute(and, vm.getValue(it), vm.getValue(10 - it))?.value
            )
        }

        (1 .. 10).forEach {
            assertEquals(
                it or (10 - it),
                vm.execute(or, vm.getValue(it), vm.getValue(10 - it))?.value
            )
        }
    }

    @Test
    fun testShift() {
        val src = """
            class Test {
                static int shiftRight(int a) {
                    return a >> 1;
                }
                
                static int unsignedShiftRight(int a) {
                    return a >>> 1;
                }
                
                static int shiftLeft(int a) {
                    return a << 1;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val shiftRight = module.getProcedure("shiftRight")
        val unsignedShiftRight = module.getProcedure("unsignedShiftRight")
        val shiftLeft = module.getProcedure("shiftLeft")

        val vm = IVirtualMachine.create()

        (1 .. 10).forEach { assertEquals(it shr 1, vm.execute(shiftRight, vm.getValue(it))?.value) }
        (1 .. 10).forEach { assertEquals(it ushr 1, vm.execute(unsignedShiftRight, vm.getValue(it))?.value) }
        (1 .. 10).forEach { assertEquals(it shl 1, vm.execute(shiftLeft, vm.getValue(it))?.value) }
    }

    @Test
    fun testStudentSubmission() {
        val src = """
            class Test {
                static int invertInt(int a){
                    int invert = 0;
                    while (a != 0) {
                        int digito = a & 10;
                        invert = invert * 10 + digito;
                        a /= 10;
                    }
                    return invert;
                }
            }
        """.trimIndent()
        val module = assertDoesNotThrow { Java2Strudel().load(src) }
        println(module)
    }
}
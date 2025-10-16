package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.tests.procedure
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IExpressionHolder
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.vm.IValue

class TestEqualsArrays {
    @Test
    fun testEqual() {
        val code = """
        class Test {
            static boolean areEqual(int[] a, int[] b) {
                return a == b;  
            }
        }
        """.trimIndent()
        val model = Java2Strudel(checkJavaCompilation = false).load(code)
        println(model)
        val vm = IVirtualMachine.create()
        val a1 = vm.allocateArrayOf(INT, 1, 2, 3)
        val a2 = vm.allocateArrayOf(INT, 1, 2, 3)
        val resFalse = vm.execute(model.procedure("areEqual"), a1, a2)
        assertTrue(resFalse?.toBoolean() == false)

        val resTrue = vm.execute(model.procedure("areEqual"), a1, a1)
        assertTrue(resTrue?.toBoolean() == true)
    }

    @Test
    fun testDifferent() {
        val code = """
        class Test {
            static boolean areDifferent(int[] a, int[] b) {
                return a != b;  
            }
        }
        """.trimIndent()
        val model = Java2Strudel(checkJavaCompilation = false).load(code)
        println(model)
        val vm = IVirtualMachine.create()
        val a1 = vm.allocateArrayOf(INT, 1, 2, 3)
        val a2 = vm.allocateArrayOf(INT, 1, 2, 3)
        val resFalse = vm.execute(model.procedure("areDifferent"), a1, a2)
        assertTrue(resFalse?.toBoolean() == true)

        val resTrue = vm.execute(model.procedure("areDifferent"), a1, a1)
        assertTrue(resTrue?.toBoolean() == false)
    }
}
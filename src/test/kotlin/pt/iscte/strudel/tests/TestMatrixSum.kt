package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import kotlin.test.assertEquals

class TestMatrixSum : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT, "sum") {
        val m = Param(INT.array().array().reference(), "m")
        val s = Var(INT, "s", 0)
        val i = Var(INT, "i", 0)
        While(exp(i) smaller m.length()) {
            val j = Var(INT, "j", 0)
            While(exp(j) smaller m[i.expression()].length()) {
                Assign(s, s + m[i.expression()][j.expression()])
                Assign(j, j + 1)
            }
            Assign(i, i + 1)
        }
        Return(s)
    }
}){


    @Test
    fun `test result and var history`() {
        val m = vm.allocateArrayOf(array(INT), vm.allocateArrayOf(INT, 1,2,3), vm.allocateArrayOf(INT, 4,5,6,7))
        trackVarHistory("s", 0, 1, 3, 6, 10, 15, 21, 28)
        trackVarHistory("i", 0, 1, 2)
        trackVarHistory("j", 0, 1, 2, 3, 0, 1, 2, 3, 4)
        val ret = vm.execute(procedure, m)
        assertEquals(28, ret!!.toInt())
    }

    @Test
    fun `test empty`() {
        val m = vm.allocateArrayOf(array(INT))
        trackVarHistory("s", 0)
        trackVarHistory("i", 0)
        trackVarHistory("j")
        val ret = vm.execute(procedure, m)
        assertEquals(0, ret!!.toInt())
    }

    // TODO CFG
}
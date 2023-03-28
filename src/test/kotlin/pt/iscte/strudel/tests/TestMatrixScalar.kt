package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import kotlin.test.assertEquals

class TestMatrixScalar : pt.iscte.strudel.tests.BaseTest({
    Procedure(VOID, "scale") {
        val m = Param(array(array(DOUBLE)), "m")
        val s = Param(DOUBLE, "s")
        val i = Var(INT, "i")
        Assign(i, 0)
        While(exp(i) smaller m.length()) {
            val j = Var(INT, "j")
            Assign(j, 0)
            While(exp(j) smaller m[i.expression()].length()) {
                ArraySet(m[i.expression()], j.expression(), m[i.expression()][j.expression()] * s.expression())
                Assign(j, j + 1)
            }
            Assign(i, i + 1)
        }
    }
}){


    @Test
    fun `test result and var history`() {
        val ref = vm.allocateArrayOf(array(DOUBLE), vm.allocateArrayOf(DOUBLE,1.5,2.5,3.0), vm.allocateArrayOf(DOUBLE,4.0,5.5,6.5,7.5))
        trackVarHistory("i", 0, 1, 2)
        trackVarHistory("j", 0, 1, 2, 3, 0, 1, 2, 3, 4)
        vm.execute(procedure, ref, vm.getValue(2.0))
        val matrix = ref.target as IArray
        val expected = arrayOf(3.0,5.0,6.0,8.0,11.0,13.0,15.0)
        var n = 0
        var l = 2
        for(i in 0..1) {
            for (j in 0..l) {
                val e = ((matrix.getElement(i) as IReference<*>).target as IArray).getElement(j).toDouble()
                assertEquals(expected[n++], e, 0.0)
            }
            l++
        }
    }


    // TODO CFG
}
package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import kotlin.test.assertEquals

class TestMatrixIdentity : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT.array(2).reference(), "identity") {
        val n = Param(INT, "n")
        val id = Var(INT.array(2).reference(), "id")
        Assign(id, INT.array().heapAllocation(listOf(n.expression(), n.expression())))
        val i = Var(INT, "i")
        Assign(i, 0)
        While(exp(i) smaller n.expression()) {
            ArraySet(id[i.expression()], i.expression(), lit(1))
            Assign(i, i + 1)
        }
        Return(id)
    }
}){
    @Test
    fun `test result and var history`() {
        trackVarHistory("i", 0, 1, 2, 3, 4)
        val ret = (vm.execute(procedure, vm.getValue(4)) as IReference<*>).target as IArray
        for(i in 0..3)
            for(j in 0..3) {
                val e =((ret.getElement(i) as IReference<*>).target as IArray).getElement(j).toInt()
                if (i == j)
                    assertEquals(1, e)
                else
                    assertEquals(0, e)
            }
    }

}
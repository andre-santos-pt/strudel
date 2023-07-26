package pt.iscte.strudel.tests.temp

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.tests.BaseTest

class TestMatrix3d : BaseTest({
    Procedure(INT.array(3).reference(), "rgbMatrix") {
        val n = Param(INT, "n")
        val img = Var(INT.array(2).reference(), "id")
        Assign(img, INT.array().heapAllocation(listOf(n.expression(), n.expression(), lit(3))))
        Return(img)
    }
}){
    @Test
    fun `test result and var history`() {
        println(procedure)
    }

}
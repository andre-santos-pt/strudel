package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.UnaryOperator
import kotlin.test.assertEquals

class TestAbsDouble : pt.iscte.strudel.tests.BaseTest({
    Procedure(DOUBLE, "abs") {
        val value = Param(DOUBLE, "value")
        If(value.expression() smaller  lit(0)) {
            Return(UnaryOperator.MINUS.on(value.expression()))
        }.Else {
            Return(value.expression())
        }
    }
}){

    @Test
    fun `test abs negative`() {
        val ret = vm.execute(procedure, vm.getValue(-2.3))
        assertEquals(2.3, ret!!.toDouble(), .00001)
    }

    @Test
    fun `test abs positive`() {
        val ret = vm.execute(procedure, vm.getValue(2.4))
        assertEquals(2.4, ret!!.toDouble(), .00001)
    }
}
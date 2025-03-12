package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.UnaryOperator
import kotlin.test.assertEquals

class TestAbsInt : BaseTest({
    Procedure(INT, "abs") {
        val value = Param(INT, "value")
        If(value.expression() smaller  lit(0)) {
            Return(UnaryOperator.MINUS.on(value.expression()))
        }.Else {
            Return(value.expression())
        }
    }
}){

    @Test
    fun `test abs negative`() {
        val ret = vm.execute(procedure, vm.getValue(-2))
        assertEquals(2, ret!!.toInt())
    }

    @Test
    fun `test abs positive`() {
        val ret = vm.execute(procedure, vm.getValue(2))
        assertEquals(2, ret!!.toInt())
    }
}
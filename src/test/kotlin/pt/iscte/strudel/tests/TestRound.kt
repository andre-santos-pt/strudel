package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.model.dsl.plus
import pt.iscte.strudel.model.util.UnaryOperator
import kotlin.test.assertEquals

class TestRound : BaseTest({
    Procedure(INT, "round") {
        val value = Param(DOUBLE, "value")
        Return(UnaryOperator.CAST_TO_INT.on(value.expression() + 0.5))
    }
}){

    @Test
    fun `test round down`() {
        val ret = vm.execute(procedure, vm.getValue(4.3))
        assertEquals(4, ret!!.toInt())
    }

    @Test
    fun `test round up`() {
        val ret = vm.execute(procedure, vm.getValue(4.5))
        assertEquals(5, ret!!.toInt())
    }
}
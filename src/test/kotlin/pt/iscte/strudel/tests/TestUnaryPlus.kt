package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.model.util.UnaryOperator
import org.junit.jupiter.api.Assertions.assertEquals

class TestUnaryPlus : BaseTest({
    Procedure(INT, "useless") {
        val value = Param(INT, "value")
        Return(UnaryOperator.PLUS. on(value.expression()))
    }
}){

    @Test
    fun `test unary plus negative`() {
        val ret = vm.execute(procedure, vm.getValue(-2))
        assertEquals(-2, ret!!.toInt())
    }

    @Test
    fun `test unary plus positive`() {
        val ret = vm.execute(procedure, vm.getValue(2))
        assertEquals(2, ret!!.toInt())
    }
}
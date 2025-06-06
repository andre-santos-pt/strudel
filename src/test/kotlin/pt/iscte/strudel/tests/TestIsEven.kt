package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.ArithmeticOperator
import org.junit.jupiter.api.Assertions.assertEquals

class TestIsEven : BaseTest({
    Procedure(BOOLEAN, "isEven") {
        val value = Param(INT, "value")
        Return(ArithmeticOperator.MOD.on(value.expression(), lit(2)) equal lit(0))
    }
}){

    @Test
    fun `test is even`() {
        val ret = vm.execute(procedure, vm.getValue(4))
        assertEquals(true, ret!!.toBoolean())
    }

    @Test
    fun `test not is even`() {
        val ret = vm.execute(procedure, vm.getValue(5))
        assertEquals(false, ret!!.toBoolean())
    }
}
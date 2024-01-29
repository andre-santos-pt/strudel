package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.model.impl.Conditional
import pt.iscte.strudel.model.util.RelationalOperator
import kotlin.test.assertEquals

class TestConditional : BaseTest({
    Procedure(INT, "max") {
        val a = Param(INT, "a")
        val b = Param(INT, "b")
        Return(
            Conditional(
                RelationalOperator.GREATER.on(a.expression(), b.expression()),
                a.expression(),
                b.expression()
            )
        )
    }
}) {

    @Test
    fun `test first max`() {
        val a =  vm.getValue(5)
        val b =  vm.getValue(3)
        val ret = vm.execute(procedure, a, b)
        assertEquals(a.toInt(), ret?.toInt())
    }

    @Test
    fun `test second max`() {
        val a =  vm.getValue(2)
        val b =  vm.getValue(3)
        val ret = vm.execute(procedure, a, b)
        assertEquals(b.toInt(), ret?.toInt())
    }
}

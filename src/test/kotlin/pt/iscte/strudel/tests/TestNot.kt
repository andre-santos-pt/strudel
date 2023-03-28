package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.model.util.UnaryOperator
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestNot : pt.iscte.strudel.tests.BaseTest({
    Procedure(BOOLEAN, "not") {
        val value = Param(BOOLEAN, "value")
        Return(UnaryOperator.NOT.on(value.expression()))
    }
}) {

    @Test
    fun `test true`() {
        val ret = vm.execute(procedure, vm.getValue(true))
        assertFalse(ret!!.toBoolean())
    }

    @Test
    fun `test false`() {
        val ret = vm.execute(procedure, vm.getValue(false))
        assertTrue(ret!!.toBoolean())
    }
}
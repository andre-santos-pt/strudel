package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.NULL_LITERAL
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.model.dsl.lit
import pt.iscte.strudel.model.impl.Conditional
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.vm.NULL
import kotlin.test.assertEquals

class TestConditionalOr : BaseTest({
    Procedure(BOOLEAN, "hasNoElements") {
        val a = Param(INT.array(), "a")
        Return(
            LogicalOperator.OR.on(
                RelationalOperator.EQUAL.on(a.expression(), NULL_LITERAL),
                RelationalOperator.EQUAL.on(a.expression().length(), lit(0))
            )
        )
    }
}) {

    @Test
    fun `test not null`() {
        val a =  vm.allocateArray(INT, 10)
        val ret = vm.execute(procedure, a)
        assert(ret?.toBoolean() == false)
    }

    @Test
    fun `test not null empty`() {
        val a =  vm.allocateArray(INT, 0)
        val ret = vm.execute(procedure, a)
        assert(ret?.toBoolean() == true)
    }

    @Test
    fun `test null`() {
        val n = vm.getNullReference()
        val ret = vm.execute(procedure, n)
        assert(ret?.toBoolean() == true)
    }
}

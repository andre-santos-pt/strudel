package pt.iscte.strudel.tests.temp

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.tests.BaseTest
import pt.iscte.strudel.tests.procedure

class TestStackRecord: BaseTest({
    val type = Record("Stack") {
        Field(INT.array(), "elements")
        Field(INT, "next")
    }

    Procedure(type.reference(), "init") {
        Param(INT, "capacity")
        val stack = type.heapAllocation()
        Return(stack)
    }

    Procedure(VOID, "push") {
        Param(type, "stack")
        Param(INT, "element")

    }



}) {

    @Test
    fun `init`() {
        vm.execute(procedure, vm.getValue(10))
        val push = module.procedure("push")
        vm.execute(push, vm.getValue(5))
        vm.execute(push, vm.getValue(6))
    }
}
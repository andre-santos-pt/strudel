package pt.iscte.strudel.tests.temp

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.tests.BaseTest

class TestStackRecord: BaseTest({
    val type = Record("Stack") {
        Field(INT.array(), "elements")
        Field(INT, "next")
    }

    Procedure(type.reference(), "init") {
        val capacity = Param(INT, "capacity")
        val stack = type.heapAllocation()
        Return(stack)
    }

    Procedure(VOID, "push") {
        val stack = Param(type, "stack")
        val e = Param(INT, "element")

    }



}) {

    @Test
    fun `init`() {
        val ret = vm.execute(procedure, vm.getValue(10))
        val push = procedure("push")
        vm.execute(push, vm.getValue(5))
        vm.execute(push, vm.getValue(6))
    }
}
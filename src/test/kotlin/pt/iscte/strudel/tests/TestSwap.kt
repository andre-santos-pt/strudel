package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.ArrayIndexError
import kotlin.test.assertTrue

class TestSwap : pt.iscte.strudel.tests.BaseTest({
    Procedure(VOID, "swap") {
        val a = Param(array(INT), "a")
        val i = Param(INT, "i")
        val j = Param(INT, "j")

        val t = Var(INT, "t")
        Assign(t, a[i.expression()])
        ArraySet(a, i.expression(), a[j.expression()])
        ArraySet(a, j.expression(), t.expression())
    }
}){

    @Test
    fun `modification`() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        vm.execute(procedure, a, vm.getValue(1), vm.getValue(3))
        a.checkArrayContent(2, 8, 6, 4)
    }

    @Test
    fun `unchanged`() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        vm.execute(procedure, a, vm.getValue(1), vm.getValue(1))
        a.checkArrayContent(2, 4, 6, 8)
    }

    @Test
    fun `out of bounds (lower)`() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        vm.execute(procedure, a, vm.getValue(-1), vm.getValue(3))
        assertTrue(vm.error is ArrayIndexError && (vm.error as ArrayIndexError).invalidIndex == -1)
    }

    @Test
    fun `out of bounds (upper)`() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        vm.execute(procedure, a, vm.getValue(1), vm.getValue(6))
        assertTrue(vm.error is ArrayIndexError && (vm.error as ArrayIndexError).invalidIndex == 6)
    }


    // TODO out of bounds
    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {

    }
}
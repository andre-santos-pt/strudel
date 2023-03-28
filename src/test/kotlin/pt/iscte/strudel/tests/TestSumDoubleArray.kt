package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.cfg.IStatementNode
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import kotlin.test.assertTrue

class TestSumDoubleArray : BaseTest({
    Procedure(DOUBLE, "sum") {
        val a = Param(array(DOUBLE), "a")
        val s = Var(DOUBLE, "s")
        Assign(s, 0.0)
        val i = Var(INT, "i")
        Assign(i, 0)
        While(i.expression() smaller a.length()) {
            Assign(s, s + a[i.expression()] )
            Assign(i, i + 1)
        }
        Return(s)
    }
}){

    @Test
    fun `empty array`() {
        assertTrue(vm.execute(procedure, vm.allocateArray(DOUBLE, 0))!!.toDouble() == 0.0)
    }

    @Test
    fun `track var history`() {
        val a = vm.allocateArrayOf(DOUBLE, 2.5, 4.5, 6.5, 8.5)
        vm.addListener(TrackDoubleVar(procedure.find(IVariableDeclaration::class, 0), 0.0, 2.5, 7.0, 13.5, 22.0))
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 1), 0, 1, 2, 3, 4))
        val ret = vm.execute(procedure, a)
        assertTrue(ret!!.toDouble() == 22.0)
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val s_sum = cfg.newStatement(cfg.entryNode, p.find(IVariableAssignment::class, 0))
        val s_i: IStatementNode = cfg.newStatement(s_sum, p.find(IVariableAssignment::class, 1))
        val b_loop = cfg.newBranch(s_i, p.find(ILoop::class))

        val s_ass1: IStatementNode = cfg.newStatement(p.find(IVariableAssignment::class, 2))
        b_loop.setBranch(s_ass1)

        val s_ass2: IStatementNode = cfg.newStatement(s_ass1, p.find(IVariableAssignment::class, 3))
        s_ass2.next = b_loop

        val s_ret: IStatementNode = cfg.newStatement(b_loop, p.find(IReturn::class))
        s_ret.next = cfg.exitNode
    }
}
package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import kotlin.test.assertTrue

class TestCharArrayCount : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT, "count") {
        val a = Param(CHAR.array().reference(), "a")
        val n = Param(CHAR, "n")

        val c = Var(INT, "c")
        Assign(c, 0)

        val i = Var(INT, "i")
        Assign(i, 0)

        While(i.expression() smaller a.expression().length()) {
            If(a.expression().element(i.expression()) equal n.expression()) {
                Assign(c, c.expression() + 1)
            }
            Assign(i, i.expression() + 1)
        }
        Return(c)
    }
}){

    @Test
    fun `empty array`() {
        assertTrue(vm.execute(
            procedure,
            vm.allocateArray(CHAR, 0),
            vm.getValue('a')
        )!!.toInt() == 0)
    }

    @Test
    fun `no occurences`() {
        assertTrue(vm.execute(
            procedure,
            vm.allocateArrayOf(CHAR, 'a', 'a', 'b', 'a'),
            vm.getValue('c')
        )!!.toInt() == 0)
    }

    @Test
    fun `track var history`() {
        val a = vm.allocateArrayOf(CHAR, 'a', 'a', 'b', 'a', 'c', 'd')
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2, 3))
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 1), 0, 1, 2, 3, 4, 5, 6))
        val ret = vm.execute(procedure, a, vm.getValue('a'))
        assertTrue(ret!!.toInt() == 3)
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val cAss = cfg.newStatement(p.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = cAss

        val s_iAss = cfg.newStatement(p.find(IVariableAssignment::class, 1))
        cAss.next = s_iAss

        val b_loop = cfg.newBranch(p.find(ILoop::class))
        s_iAss.next = b_loop

        val b_ifstat = cfg.newBranch(p.find(ISelection::class))
        b_loop.setBranch(b_ifstat)

        val s_cAss_ = cfg.newStatement(p.find(IVariableAssignment::class, 2))
        b_ifstat.setBranch(s_cAss_)

        val s_iInc = cfg.newStatement(p.find(IVariableAssignment::class, 3))
        s_cAss_.next = s_iInc
        b_ifstat.next = s_iInc

        s_iInc.next = b_loop

        val s_ret = cfg.newStatement(p.find(IReturn::class))
        b_loop.next = s_ret

        s_ret.next = cfg.exitNode
    }
}
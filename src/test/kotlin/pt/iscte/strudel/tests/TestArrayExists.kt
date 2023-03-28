package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestArrayExists : pt.iscte.strudel.tests.BaseTest({
    Procedure(BOOLEAN, "find") {
        val a = Param(array(INT), "a")
        val e = Param(INT, "e")

        val i = Var(INT, "i")
        Assign(i, 0)
        While(exp(i) smaller a.length()) {
            If(a[exp(i)] equal  exp(e)) {
                Return(True)
            }
            Assign(i, i + 1)
        }
        Return(False)
    }
}){


    @Test
    fun `check positive`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 8, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2, 3))
        val ret = vm.execute(procedure, a, vm.getValue(10))
        assertTrue(ret!!.toBoolean())
    }

    @Test
    fun `check negative`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 8, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2, 3, 4, 5, 6))
        val ret = vm.execute(procedure, a, vm.getValue(7))
        assertFalse(ret!!.toBoolean())
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val iInit = cfg.newStatement(procedure.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = iInit
        val loop = cfg.newBranch(iInit, procedure.find(ILoop::class))
        val iff = cfg.newBranch(loop, procedure.find(ISelection::class))
        loop.setBranch(iff)
        val retTrue = cfg.newStatement(iff, procedure.find(IReturn::class, 0))
        retTrue.next = cfg.exitNode
        iff.setBranch(retTrue)
        val iInc = cfg.newStatement(iff, procedure.find(IVariableAssignment::class, 1))
        iInc.next = loop
        val retFalse = cfg.newStatement(loop, procedure.find(IReturn::class, 1))
        loop.next = retFalse
        retFalse.next = cfg.exitNode
    }
}
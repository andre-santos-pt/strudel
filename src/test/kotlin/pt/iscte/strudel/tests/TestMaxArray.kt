package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import kotlin.test.assertTrue

class TestMaxArray : BaseTest({
    Procedure(INT, "max") {
        val a = Param(array(INT), "a")
        val m = Var(INT, "m")
        Assign(m, a[lit(0)])
        val i = Var(INT, "i")
        Assign(i, 1)
        While(exp(i) smaller a.length()) {
            If(a[exp(i)] greater exp(m)) {
                Assign(m, a[exp(i)] )
            }
            Assign(i, i + 1)
        }
        Return(m)
    }
}){


    @Test
    fun `track var history`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 8, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 2, 9, 10))
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 1), 1, 2, 3, 4, 5, 6))
        val ret = vm.execute(procedure, a)
        assertTrue(ret!!.toInt() == 10)
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val mInit = cfg.newStatement(procedure.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = mInit
        val iInit = cfg.newStatement(mInit, procedure.find(IVariableAssignment::class, 1))
        val loop = cfg.newBranch(iInit, procedure.find(ILoop::class))
        val iff = cfg.newBranch(loop, procedure.find(ISelection::class))
        loop.setBranch(iff)
        val mAss = cfg.newStatement(iff, procedure.find(IVariableAssignment::class, 2))
        iff.setBranch(mAss)
        val iInc = cfg.newStatement(iff, procedure.find(IVariableAssignment::class, 3))
        mAss.next = iInc
        iInc.next = loop
        val ret = cfg.newStatement(loop, procedure.find(IReturn::class))
        ret.next = cfg.exitNode
    }
}
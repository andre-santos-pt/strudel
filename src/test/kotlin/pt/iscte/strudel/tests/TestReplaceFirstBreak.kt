package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class TestReplaceFirstBreak : BaseTest({
    Procedure(BOOLEAN, "replaceFirst") {
        val a = Param(array(INT), "a")
        val e = Param(INT, "e")
        val i = Var(INT, "i")
        Assign(i, 0)
        While(exp(i) smaller a.length()) {
            If(a[exp(i)] equal exp(e)) {
                ArraySet(a, exp(i), lit(0))
                Break()
            }
            Assign(i, i + 1)
        }
        Return(exp(i).notEqual(a.length()))
    }
}){


    @Test
    fun `one replace`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 6, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2))
        val ret = vm.execute(procedure, a, vm.getValue(6))
        a.checkIntArrayContent(2, 9, 0, 10, 6, 1)
        assertTrue(ret!!.toBoolean())
    }

    @Test
    fun `no hits`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 6, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2, 3, 4, 5, 6))
        val ret = vm.execute(procedure, a, vm.getValue(7))
        a.checkIntArrayContent(2, 9, 6, 10, 6, 1)
        assertFalse(ret!!.toBoolean())
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val iInit = cfg.newStatement(procedure.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = iInit
        val loop = cfg.newBranch(iInit, procedure.find(ILoop::class))
        val iff = cfg.newBranch(loop, procedure.find(ISelection::class))
        loop.setBranch(iff)
        val ass = cfg.newStatement(iff, procedure.find(IArrayElementAssignment::class, 0))
        iff.setBranch(ass)
        val brk = cfg.newStatement(ass, procedure.find(IBreak::class, 0))
        val ret = cfg.newStatement(loop, procedure.find(IReturn::class, 0))
        ret.next = cfg.exitNode
        brk.next = ret
        val iInc = cfg.newStatement(iff, procedure.find(IVariableAssignment::class, 1))
        iInc.next = loop
        loop.next = ret
    }
}
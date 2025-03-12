package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find

class TestReplaceFirstReturn : BaseTest({
    Procedure(VOID, "replaceFirst") {
        val a = Param(array(INT), "a")
        val e = Param(INT, "e")
        val i = Var(INT, "i")
        Assign(i, 0)
        While(exp(i) smaller a.length()) {
            If(a[exp(i)] equal exp(e)) {
                ArraySet(a, exp(i), lit(0))
                ReturnVoid()
            }
            Assign(i, i + 1)
        }
    }
}){


    @Test
    fun `one replace`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 6, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2))
        vm.execute(procedure, a, vm.getValue(6))
        a.checkIntArrayContent(2, 9, 0, 10, 6, 1)
    }

    @Test
    fun `no hits`() {
        val a = vm.allocateArrayOf(INT, 2, 9, 6, 10, 6, 1)
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 0), 0, 1, 2, 3, 4, 5, 6))
        vm.execute(procedure, a, vm.getValue(7))
        a.checkIntArrayContent(2, 9, 6, 10, 6, 1)
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val iInit = cfg.newStatement(procedure.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = iInit
        val loop = cfg.newBranch(iInit, procedure.find(ILoop::class))
        val iff = cfg.newBranch(loop, procedure.find(ISelection::class))
        loop.setBranch(iff)
        val ass = cfg.newStatement(iff, procedure.find(IArrayElementAssignment::class, 0))
        val ret = cfg.newStatement(ass, procedure.find(IReturn::class, 0))
        ret.next = cfg.exitNode
        iff.setBranch(ass)
        val iInc = cfg.newStatement(iff, procedure.find(IVariableAssignment::class, 1))
        iInc.next = loop
        loop.next = cfg.exitNode
    }
}
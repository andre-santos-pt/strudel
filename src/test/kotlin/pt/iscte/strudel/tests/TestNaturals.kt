package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestNaturals : BaseTest({
    Procedure(INT.array().reference(), "naturals") {
        val n = Param(INT, "n")
        val a = Var(INT.array().reference(), "a")
        Assign(a, INT.array().heapAllocation(n.expression()))
        val i = Var(INT, "i")
        Assign(i, 0)
        While(i.expression() smaller n.expression()) {
            ArraySet(a, i.expression(), i + 1)
            Assign(i, i + 1)
        }
        Return(a)
    }
}) {

    @Test
    fun `empty array`() {
        val ret = vm.execute(procedure, vm.getValue(0))
        ret!!.checkIntArrayContent()
    }

    @Test
    fun `check array content`() {
        vm.addListener(
            TrackIntVar(
                procedure.find(
                    IVariableDeclaration::class,
                    1
                ), 0, 1, 2, 3, 4, 5, 6, 7
            )
        )
        val ret = vm.execute(procedure, vm.getValue(7))
        ret!!.checkIntArrayContent(1, 2, 3, 4, 5, 6, 7)
    }

    @Test
    fun `array allocation listener`() {
        val LEN = 7
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                assertTrue(ref.target is IArray)
                assertEquals((ref.target as IArray).length, LEN)
            }
        })
        vm.execute(procedure, vm.getValue(LEN))
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph) {
        val arrayInit = cfg.newStatement(p.find(IVariableAssignment::class, 0))
        cfg.entryNode.next = arrayInit

        val iteratorInit =
            cfg.newStatement(p.find(IVariableAssignment::class, 1))
        arrayInit.next = iteratorInit

        val whileLoop = cfg.newBranch(p.find(ILoop::class))
        iteratorInit.next = whileLoop

        val arrayVarAssignment =
            cfg.newStatement(p.find(IArrayElementAssignment::class))
        whileLoop.setBranch(arrayVarAssignment)

        val arrayIteratorIncrement =
            cfg.newStatement(p.find(IVariableAssignment::class, 2))
        arrayVarAssignment.next = arrayIteratorIncrement
        arrayIteratorIncrement.next = whileLoop

        val returnStatement = cfg.newStatement(p.find(IReturn::class))
        whileLoop.next = returnStatement

        returnStatement.next = cfg.exitNode
    }
}
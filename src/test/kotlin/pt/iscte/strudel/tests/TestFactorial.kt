package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IBranchNode
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFactorial : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT, "factorial") {
        val n = Param(INT, "n")
        If(n.expression() equal lit(1)) {
            Return(n.expression())

        }.Else {
            Return(n.expression() * this@Procedure.procedure!!.expression(n.expression() - 1))
        }
    }
}, callStackMaximum = 5){

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {
        val ifGuard: IBranchNode = cfg.newBranch(p.find(ISelection::class))
        cfg.entryNode.next = ifGuard

        val firstReturn = cfg.newStatement(p.find(IReturn::class, 0))
        ifGuard.setBranch(firstReturn)
        firstReturn.next = cfg.exitNode

        val secondReturn = cfg.newStatement(p.find(IReturn::class,1))
        ifGuard.next = secondReturn
        secondReturn.next = cfg.exitNode
    }

    @Test
    fun `base case`() {
        assertTrue(call("1")!!.toInt() == 1)
    }

    @Test
    fun `call stack count`() {
        var calls = 0
        var returns = 0

        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(
                p: IProcedureDeclaration,
                args: List<IValue>,
                caller: IProcedureDeclaration?
            ) {
                println(args)
                calls++
            }

            override fun procedureEnd(p: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
                returns++
            }
        })

        val ret = call("5")
        assertEquals(5, calls)
        assertEquals(5, returns)
        assertEquals(120, ret!!.toInt())
    }

    @Test
    fun `stack overflow`() {
        call("6")
        assertTrue(vm.error!!.type == RuntimeErrorType.STACK_OVERFLOW)
    }
}


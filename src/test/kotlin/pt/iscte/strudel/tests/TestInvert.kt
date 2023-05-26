package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertTrue

class TestInvert : BaseTest({
    val swap = Procedure(VOID, "swap") {
        val a = Param(array(INT), "a")
        val i = Param(INT, "i")
        val j = Param(INT, "j")

        val t = Var(INT, "t")
        Assign(t, a[i.expression()])
        ArraySet(a, i.expression(), a[j.expression()])
        ArraySet(a, j.expression(), t.expression())
    }

    Procedure(VOID, "invert") {
        val a = Param(array(INT), "a")

        val i = Var(INT, "i")
        Assign(i, 0)
        While(i.expression() smaller (a.length() / 2)) {
            Call(swap, a.expression(), i.expression(), a.length() - 1 - i.expression())
            Assign(i, i + 1)
        }
    }
}){

    @Test
    fun `empty`() {
        val a = vm.allocateArrayOf(INT)
        vm.execute(procedure, a)
        a.checkArrayContent()
    }

    @Test
    fun `single`() {
        val a = vm.allocateArrayOf(INT, 1)
        vm.execute(procedure, a)
        a.checkArrayContent(1)
    }

    @Test
    fun `modification even`() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        TrackIntVar(procedure.find(IVariableDeclaration::class), 0, 1)
        val l = object : IVirtualMachine.IListener {
            var depth = 0
            var maxDepth = 0
            override fun procedureCall(p: IProcedureDeclaration, args: List<IValue>) {
                depth++
                if(depth > maxDepth)
                    maxDepth = depth
            }

            override fun procedureEnd(p: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
                depth--
            }
        }
        vm.addListener(l)
        vm.execute(procedure, a)
        a.checkArrayContent(8, 6, 4, 2)
        assertTrue(l.depth == 0)
        assertTrue(l.maxDepth == 2)
    }

    @Test
    fun `modification odd`() {
        val a = vm.allocateArrayOf(INT, 0, 2, 4, 6, 8)
        vm.execute(procedure, a)
        a.checkArrayContent(8, 6, 4, 2, 0)
    }


    // TODO out of bounds
    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {

    }
}
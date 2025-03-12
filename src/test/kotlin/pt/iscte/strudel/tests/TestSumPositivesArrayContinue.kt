package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import kotlin.test.assertTrue

class TestSumPositivesArrayContinue : BaseTest({
    Procedure(DOUBLE, "sum") {
        val a = Param(array(DOUBLE), "a")
        val s = Var(DOUBLE, "s")
        Assign(s, 0.0)
        val i = Var(INT, "i")
        Assign(i, 0)
        While(i.expression() smaller a.length()) {
            If(a[i.expression()] smaller lit(0)) {
                Assign(i, i + 1)
                Continue()
            }
            Assign(s, s + a[i.expression()] )
            Assign(i, i + 1)
        }
        Return(s)
    }
}){

    @Test
    fun `track var history`() {
        val a = vm.allocateArrayOf(DOUBLE, 2.5, -4.5, 6.5, 8.5)
        vm.addListener(TrackDoubleVar(procedure.find(IVariableDeclaration::class, 0), 0.0, 2.5, 9.0, 17.5))
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 1), 0, 1, 2, 3, 4))
        val ret = vm.execute(procedure, a)
        assertTrue(ret!!.toDouble() == 17.5)
    }

    override fun fillCFG(p: IProcedure, cfg: IControlFlowGraph)  {

    }
}
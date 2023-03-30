package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAvgInvoke : pt.iscte.strudel.tests.BaseTest({
    val sumProcedure = Procedure(DOUBLE, "sum") {
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

    Procedure(DOUBLE, "avg") {
        val a = Param(array(DOUBLE), "a")
        Return(callExpression(sumProcedure, a.expression()) / a.length())
    }
}){

    @Test
    fun `result`() {
        val a = vm.allocateArrayOf(DOUBLE, 14.2, 14.4, 14.6)
        vm.execute(procedure, a)
        assertTrue(vm.execute(procedure, a)!!.toDouble() == 14.4)
    }



    @Test
    fun `invocationEvent`() {
        val a = vm.allocateArrayOf(DOUBLE, 14.2, 14.4, 14.6)
        vm.addListener(object : IVirtualMachine.IListener {
            var stack = 0
            override fun procedureCall(p: IProcedureDeclaration, args: List<IValue>) {
                if(stack == 0)
                    assertEquals(module.procedures.find { it.id == "avg" }!!, p)
                else if(stack == 1)
                    assertEquals(module.procedures.find { it.id == "sum" }!!, p)
                stack++
            }

            override fun procedureEnd(p: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
                if(stack == 1)
                   assertEquals(module.procedures.find { it.id == "avg" }!!, p)
                else if(stack == 2)
                    assertEquals(module.procedures.find { it.id == "sum" }!!, p)
                stack--
            }
        })
        vm.execute(procedure, a)

    }

}
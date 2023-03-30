package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestNaturalsTemp : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT.array().reference(), "naturals") {
        val n = Param(INT, "n")
        val a = Var(INT.array().reference(), "a")
        Assign(a, INT.array().heapAllocation(n.expression()))
        val i = Var(INT, "i")
        Assign(i, 0)
        While(i.expression() smaller n.expression()) {
            //assignArray(a,i + 1, i.expression())
            Assign(i, i + 1)
        }
        Return(a)
    }
}){

    @Test
    fun `empty array`() {
        val ret = vm.execute(procedure, vm.getValue(5))
       // ret!!.checkArrayContent()
        println(ret)
    }

    @Test
    fun `check array content`() {
        vm.addListener(TrackIntVar(procedure.find(IVariableDeclaration::class, 1), 0, 1, 2, 3, 4, 5, 6, 7))
        val ret = vm.execute(procedure, vm.getValue(7))
        ret!!.checkArrayContent(1,2,3,4,5,6,7)
    }

    @Test
    fun `array allocation listener`() {
        val LEN = 7
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                assertEquals(ref.target.length, LEN)
            }
        })
        vm.execute(procedure, vm.getValue(LEN))
    }

}
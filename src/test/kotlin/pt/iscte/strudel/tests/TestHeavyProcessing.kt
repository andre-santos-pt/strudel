package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

const val N2 = 1000

class TestHeavyProcessing : BaseTest({
    Procedure(VOID, "test") {
        val i = Var(INT, "i")
        Assign(i, 0)
        val j = Var(INT, "j")

        val c = Var(INT, "c")
        Assign(c, 0)
        While(exp(i) smaller lit(N2)) {
            Assign(j, 0)
            While(exp(j) smaller lit(N2)) {
                Assign(c, c + 1)
                Assign(j, j + 1)
            }
            Assign(i, i + 1)
        }
    }
}){

    @Test
    fun `no listener`() {
        call()
    }

    @Test
    fun `check i and j`() {
        vm.addListener(object : IVirtualMachine.IListener {
            var i = 0
            var j = 0
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                if(a.target.id == "i") {
                    val iStack = vm.callStack.topFrame.getValue("i").toInt()
                    assertEquals(i++, iStack)
                }
                else if(a.target.id == "j") {
                    val jStack = vm.callStack.topFrame.getValue("j").toInt()
                    assertEquals(j++, jStack)
                    if(jStack == N2)
                        j = 0
                }

            }
        })
        call()
    }


}
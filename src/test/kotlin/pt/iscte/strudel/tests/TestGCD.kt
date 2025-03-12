package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestGCD : BaseTest({
    Procedure(INT, "gcd") {
        val a = Param(INT, "a")
        val b = Param(INT, "b")
        If(exp(b) equal lit(0)) {
            Return(a)
        }.Else {
            Return(
                callExpression(this@Procedure.procedure!!, exp(b), ArithmeticOperator.MOD.on(
                    exp(a), exp(b)
                ))
            )
        }
    }
}){

    @Test
    fun execute() {
        var countCalls = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(s: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                countCalls++
            }
        })
        val result = vm.execute(procedure, vm.getValue(25), vm.getValue(30))
        assertEquals(5, result?.toInt())
        assertEquals(4, countCalls)
    }

}
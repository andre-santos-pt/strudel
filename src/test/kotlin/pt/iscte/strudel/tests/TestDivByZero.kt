package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDivByZeroInt : BaseTest({
    Procedure(INT, "test") {
        val a = Param(INT, "a")
        val b = Param(INT, "b")
        Return(ArithmeticOperator.DIV.on(a.expression(), b.expression()))
    }
}){


    class Listener() : IVirtualMachine.IListener {
        var hit = false
        override fun executionError(e: RuntimeError) {
            hit = true
            assertEquals(e.type, RuntimeErrorType.DIVBYZERO)
        }
    }

    @Test
    fun `test div by 0 int`() {
        val listener = Listener()
        vm.addListener(listener)
        vm.execute(procedure, vm.getValue(5), vm.getValue(0))
        assertTrue(listener.hit)
    }
}

class TestDivByZeroIntMod : BaseTest({
    Procedure(INT, "test") {
        val a = Param(INT, "a")
        val b = Param(INT, "b")
        Return(ArithmeticOperator.MOD.on(a.expression(), b.expression()))
    }
}){


    class Listener() : IVirtualMachine.IListener {
        var hit = false
        override fun executionError(e: RuntimeError) {
            hit = true
            assertEquals(e.type, RuntimeErrorType.DIVBYZERO)
        }
    }

    @Test
    fun `test remainder by 0 int`() {
        val listener = Listener()
        vm.addListener(listener)
        vm.execute(procedure, vm.getValue(5), vm.getValue(0))
        assertTrue(listener.hit)
    }
}

class TestDivByZeroDouble : BaseTest({
    Procedure(INT, "test") {
        val a = Param(DOUBLE, "a")
        val b = Param(DOUBLE, "b")
        Return(ArithmeticOperator.DIV.on(a.expression(), b.expression()))
    }
}){


    class Listener() : IVirtualMachine.IListener {
        var hit = false
        override fun executionError(e: RuntimeError) {
            hit = true
            assertEquals(e.type, RuntimeErrorType.DIVBYZERO)
        }
    }

    @Test
    fun `test div by 0 double`() {
        val listener = Listener()
        vm.addListener(listener)
        vm.execute(procedure, vm.getValue(5.0), vm.getValue(0.0))
        assertTrue(listener.hit)
    }

}
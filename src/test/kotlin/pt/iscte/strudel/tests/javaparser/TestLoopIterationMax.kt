package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestLoopIterationMax : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT, "test") {
        val n = Param(INT,"n")
        val m = Param(INT,"m")
        val i = Var(INT, "i", 0)
        val j = Var(INT, "j", 0)
        val s = Var(INT, "s", 0)
        While(i.expression() smaller n) {
            Assign(j, 0)
            While(j.expression() smaller m) {
                Assign(s, s.expression() + 1)
                Assign(j, j.expression() + 1)
            }
            Assign(i, i.expression() + 1)
        }
        Return(s)
    }
}, loopIterationMax = 999){


    class Listener : IVirtualMachine.IListener {
        var hit = false
        override fun executionError(e: RuntimeError) {
            hit = true
            assertEquals(e.type, RuntimeErrorType.LOOP_MAX)
        }
    }

    @Test
    fun `runtime error`() {
        val listener = Listener()
        vm.addListener(listener)
        vm.execute(procedure, vm.getValue(1000), vm.getValue(100))
        assertTrue(listener.hit)
    }

    @Test
    fun `no error`() {
        val listener = Listener()
        vm.addListener(listener)
        println(procedure)
        val ret = vm.execute(procedure, vm.getValue(999), vm.getValue(100))
        assertEquals(99900, ret!!.toInt())
        assertFalse(listener.hit)
    }
}
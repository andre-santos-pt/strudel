package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArrayIndexOutOfBounds : pt.iscte.strudel.tests.BaseTest({
    Procedure(INT, "get") {
        val a = Param(array(INT), "a")
        val i = Param(INT, "i")
        Return(a[i.expression()])
    }
}){


    class Listener(val invalid: Int) : IVirtualMachine.IListener {
        var hit = false
        var array: IArray? = null
        override fun executionError(e: RuntimeError) {
            hit = true
            assertTrue(e is ArrayIndexError)
            assertEquals(invalid, e.invalidIndex)
            array = e.array
        }
    }

    @Test
    fun `test lower`() {
        val listener = Listener(-1)
        val a = vm.allocateArrayOf(INT, 1,2,3,4)
        vm.addListener(listener)
        vm.execute(procedure, a, vm.getValue(-1))
        assertTrue(listener.hit)
        assertEquals(a.target, listener.array)
    }

    @Test
    fun `test zero`() {
        val listener = Listener(0)
        val a = vm.allocateArrayOf(INT)
        vm.addListener(listener)
        vm.execute(procedure, a, vm.getValue(0))
        assertTrue(listener.hit)
        assertEquals(a.target, listener.array)
    }

    @Test
    fun `test upper`() {
        val listener = Listener(4)
        val a = vm.allocateArrayOf(INT, 1,2,3,4)
        vm.addListener(listener)
        vm.execute(procedure, a, vm.getValue(4))
        assertTrue(listener.hit)
        assertEquals(a.target, listener.array)
    }
}
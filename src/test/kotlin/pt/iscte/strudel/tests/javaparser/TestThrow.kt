package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.*

class TestThrow {


    class Listener : IVirtualMachine.IListener {
        var error: RuntimeError? = null
        override fun executionError(e: RuntimeError) {
            error = e
            assertEquals(e.type, RuntimeErrorType.EXCEPTION)
        }
    }

    val code = """
        class Test {
            static int test(int n) {
                if(n < 0)
                    throw new IllegalArgumentException("value is negative");
                else if(n > 100)
                    throw new ArrayIndexOutOfBoundsException();
                else
                    return -n;
            }
        }
    """.trimIndent()

    @Test
    fun throwRuntimeException() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create(throwExceptions = false)
        val listener = Listener()
        vm.addListener(listener)
        val ret = vm.execute(model.procedures[1] as IProcedure, vm.getValue(-2))
        assertNotNull(listener.error)
        assertEquals("value is negative", listener.error?.message)
        assertNull(ret)
    }

    @Test
    fun throwRuntimeExceptionNoMessage() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create(throwExceptions = false)
        val listener = Listener()
        vm.addListener(listener)
        val ret = vm.execute(model.procedures[1] as IProcedure, vm.getValue(101))
        assertNotNull(listener.error)
        assertEquals("ArrayIndexOutOfBoundsException", listener.error?.message)
        assertNull(ret)
    }

    @Test
    fun notThrowRuntimeException() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create(throwExceptions = false)
        val listener = Listener()
        vm.addListener(listener)
        val ret = vm.execute(model.procedures[1] as IProcedure, vm.getValue(2))
        assertNull(listener.error)
        assertEquals(vm.getValue(-2).value, ret?.value)
    }
}
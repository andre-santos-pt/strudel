package pt.iscte.strudel.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class TestUsedMemory {

    @Test
    fun `Heap memory with INT`() {
        val procedure = Procedure(INT, "sum") {
            val a = Param(array(INT), "a")

            val sum = Var(INT, "sum")
            val i = Var(INT, "i")
            Assign(sum, 0)
            Assign(i, 0)

            While(exp(i) smaller a.length()) {
                Assign(sum, sum + a[exp(i)])
                Assign(i, i + 1)
            }

            Return(exp(sum))
        }

        val vm = IVirtualMachine.create()
        val arg = vm.allocateArrayOf(INT, 1, 2, 3, 4, 5) // 44 bytes (5 * 4 + 24)

        vm.execute(procedure, arg)
        assertEquals(44, vm.usedMemory)
    }

    @Test
    fun `Heap memory with DOUBLE`() {
        val procedure = Procedure(DOUBLE, "sum") {
            val a = Param(array(DOUBLE), "a")

            val sum = Var(DOUBLE, "sum")
            val i = Var(INT, "i")
            Assign(sum, 0.0)
            Assign(i, 0)

            While(exp(i) smaller a.length()) {
                Assign(sum, sum + a[exp(i)])
                Assign(i, i + 1)
            }

            Return(exp(sum))
        }

        val vm = IVirtualMachine.create()
        val arg = vm.allocateArrayOf(DOUBLE, 1.0, 2.0, 3.0, 4.0, 5.0) // 64 bytes (5 * 8 + 24)

        vm.execute(procedure, arg)
        assertEquals(64, vm.usedMemory)
    }

    @Test
    fun `Call Stack + Heap with INT`() {
        val procedure = Procedure(INT, "sum") {
            val a = Param(array(INT), "a")

            val sum = Var(INT, "sum")
            val i = Var(INT, "i")
            Assign(sum, 0)
            Assign(i, 0)

            While(exp(i) smaller a.length()) {
                Assign(sum, sum + a[exp(i)])
                Assign(i, i + 1)
            }

            Return(exp(sum))
        }

        // Expected = Overhead + sizeof(ref(a)) + sizeof(sum) + sizeof(i)
        //          = 16 + 4 + 4 + 4
        //          = 28 bytes
        var callStackMemoryBytes = 0

        val vm = IVirtualMachine.create()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureEnd(procedure: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
                callStackMemoryBytes = vm.callStack.memory
            }
        })

        val arg = vm.allocateArrayOf(INT, 1, 2, 3, 4, 5) // 44 bytes (5 * 4 + 24)

        vm.execute(procedure, arg)
        assertEquals(44, vm.usedMemory)
        assertEquals(28, callStackMemoryBytes)
    }
}
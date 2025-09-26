package pt.iscte.strudel.tests

import com.github.javaparser.utils.Utils.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.OutOfMemoryError

class TestOutOfMemoryError {

    @Test
    fun test() {
        val src = """
            class Test {
                static void foo() {
                    int[] a = new int[9999];
                }
            }
        """.trimIndent()

        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(src)

        val error = assertThrows<OutOfMemoryError> {
            vm.execute(module.getProcedure("foo"))
        }

        assertNotNull(error.source)
        assertTrue(error.source is IVariableAssignment)
        assertEquals("a", (error.source as IVariableAssignment).target.id)
        assertTrue((error.source as IVariableAssignment).expression is IArrayAllocation)
        assertEquals("9999", ((error.source as IVariableAssignment).expression as IArrayAllocation).dimensions[0].toString())
    }
}
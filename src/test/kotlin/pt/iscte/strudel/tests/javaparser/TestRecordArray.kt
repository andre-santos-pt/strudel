package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.StringType
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.model.HostRecordType
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class TestRecordArray {

    @Test
    fun testString() {
        val vm = IVirtualMachine.create()
        val array = assertDoesNotThrow { vm.allocateArrayOf(StringType.reference(), getString("hello")) }.target

        val element = array.getElement(0)
//        assertIs<HostRecordType>(element.type)
        assertEquals("hello", element.referenceValue)
    }

    @Test
    fun testNode() {
        val src = """
            class Node { }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val nodeType = module.getRecordType("Node")

        val vm = IVirtualMachine.create()

        val node1 = vm.allocateRecord(nodeType)
        val node2 = vm.allocateRecord(nodeType)
        val node3 = vm.allocateRecord(nodeType)

        val array = assertDoesNotThrow { vm.allocateArrayOf(nodeType.reference(), node1, node2, node3) }.target

        for(i in 0..2)
            assertTrue(array.getElement(i) is IReference<*>)

        assertEquals(node1, array.getElement(0))
        assertEquals(node2, array.getElement(1))
        assertEquals(node3, array.getElement(2))
    }
}
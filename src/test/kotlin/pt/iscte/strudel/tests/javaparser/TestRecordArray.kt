package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.StringType
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.model.HostRecordType
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestRecordArray {

    @Test
    fun testString() {
        val vm = IVirtualMachine.create()
        val array = assertDoesNotThrow { vm.allocateArrayOf(StringType, getString("hello")) }.target

        val element = array.getElement(0)
        assertIs<HostRecordType>(element.type)
        assertEquals("hello", element.value)
    }

    @Test
    fun testNode() {
        val src = """
            public class Node { }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val nodeType = module.getRecordType("Node")

        val vm = IVirtualMachine.create()

        val node1 = vm.allocateRecord(nodeType)
        val node2 = vm.allocateRecord(nodeType)
        val node3 = vm.allocateRecord(nodeType)

        val array = assertDoesNotThrow { vm.allocateArrayOf(nodeType, node1, node2, node3) }.target

        assertIs<IReference<IRecord>>(array.getElement(0))
        assertIs<IReference<IRecord>>(array.getElement(1))
        assertIs<IReference<IRecord>>(array.getElement(2))

        assertEquals(node1, array.getElement(0))
        assertEquals(node2, array.getElement(1))
        assertEquals(node3, array.getElement(2))
    }
}
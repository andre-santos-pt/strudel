package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestNestedType {

    @Test
    fun test() {
        val src = """
            public class LinkedList<T> {
                private class Node {
                    public T item;
                    public Node next;
                }
                
                private Node first;
                private Node last;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        val listType = assertDoesNotThrow { module.getRecordType("LinkedList") }
        val nodeType = assertDoesNotThrow { module.getRecordType("LinkedList.Node") }
        assertNotNull(listType)
        assertNotNull(nodeType)

        val vm = IVirtualMachine.create()

        val list = vm.allocateRecord(listType).target
        assertEquals(NULL, list.getField(listType["first"]))
        assertEquals(NULL, list.getField(listType["last"]))
    }
}
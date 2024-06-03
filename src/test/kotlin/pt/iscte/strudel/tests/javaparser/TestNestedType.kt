package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.javaparser.extensions.getString
import pt.iscte.strudel.vm.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestNestedType {

    @Test
    fun `Declared but not used`() {
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

    @Test
    fun `Not static`() {
        val src = """
            public class LinkedList<T> implements Iterable<T> {
                private class Node {
                    public T item;
                    public Node next;
                    
                    public Node(T item, Node next) {
                        this.item = item;
                        this.next = next;
                    }
                }
                
                private Node first;
                private Node last;
                
                public LinkedList(T init) {
                    first = new Node(init, null);
                    last = first;
                }
                
                public T first() {
                    return first.item;
                }
                
                private class LinkedListIterator implements Iterator<T> {
                    private Node current = first;
                    
                    public boolean hasNext() {
                        return current != null;
                    }
                    
                    public T next() {
                        T item = current;
                        current = current.next;
                        return item;
                    }
                }
            }
        """.trimIndent()
        val module = assertDoesNotThrow { Java2Strudel().load(src) }

        val listType = assertDoesNotThrow { module.getRecordType("LinkedList") }
        val nodeType = assertDoesNotThrow { module.getRecordType("LinkedList.Node") }
        val iteratorType = assertDoesNotThrow { module.getRecordType("LinkedList.LinkedListIterator") }
        assertNotNull(listType)
        assertNotNull(nodeType)
        assertNotNull(iteratorType)
    }

    @Test
    fun `Declared and used`() {
        val src = """
            public class LinkedList<T> {
                private class Node {
                    public T item;
                    public Node next;
                    
                    public Node(T item, Node next) {
                        this.item = item;
                        this.next = next;
                    }
                }
                
                private Node first;
                private Node last;
                
                public LinkedList(T init) {
                    first = new Node(init, null);
                    last = first;
                }
                
                public T first() {
                    return first.item;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        val listType = assertDoesNotThrow { module.getRecordType("LinkedList") }
        val nodeType = assertDoesNotThrow { module.getRecordType("LinkedList.Node") }
        assertNotNull(listType)
        assertNotNull(nodeType)

        val vm = IVirtualMachine.create()

        val list = vm.allocateRecord(listType)
        val constructor = module.getProcedure("\$init", "LinkedList")

        vm.execute(constructor, list, vm.getValue(12345))

        val first = list.target.getField(listType["first"]) as IReference<IRecord>
        val last = list.target.getField(listType["last"]) as IReference<IRecord>

        val firstItem = first.target.getField(nodeType["item"]) as IReference<IValue>
        val lastItem = last.target.getField(nodeType["item"]) as IReference<IValue>

        assertEquals(12345, firstItem.target.value)
        assertEquals(12345, lastItem.target.value)
    }
}
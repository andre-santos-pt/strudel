package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        println(module)

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
            import java.util.Iterator;
                
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
                
                public Iterator<T> iterator() {
                    return new LinkedListIterator();
                }
                
                private class LinkedListIterator implements Iterator<T> {
                    private Node current = first;
                    
                    public boolean hasNext() {
                        T ignored = last.item;
                        return current != null;
                    }
                    
                    public T next() {
                        T item = current.item;
                        current = current.next;
                        return item;
                    }
                }
            }
        """.trimIndent()
        val module = assertDoesNotThrow { Java2Strudel().load(src) }

        println(module)

        val listType = assertDoesNotThrow { module.getRecordType("LinkedList") }
        val nodeType = assertDoesNotThrow { module.getRecordType("LinkedList.Node") }
        val iteratorType = assertDoesNotThrow { module.getRecordType("LinkedList.LinkedListIterator") }
        assertNotNull(listType)
        assertNotNull(nodeType)
        assertNotNull(iteratorType)

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

        val iteratorMethod = module.getProcedure("iterator", "LinkedList")
        val ref = vm.execute(iteratorMethod, list)
        assertIs<IReference<IRecord>>(ref)
        val iterator: IRecord = ref.target

        val current = iterator.getField(iteratorType["current"]) as IReference<IRecord>
        assertTrue(current.target.type.isSame(nodeType))
        val currentItem = current.target.getField(nodeType["item"]) as IReference<IValue>
        assertEquals(12345, currentItem.target.value)
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

        println(module)

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
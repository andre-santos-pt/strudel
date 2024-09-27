package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestFieldInitializers {

    @Test
    fun `With Explicit Constructor`() {
        val src = """
            class Counter {
                private int count = 9;
                private int doubleCount = count * 2;
                
                public Counter() {
                    count++;
                }
                
                public Counter(int x) {
                    count += x;
                }
                
                public void inc() {
                    count++;
                }
                
                public void dec() {
                    count--;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()

        val counter = vm.allocateRecord(type)
        vm.execute(module.getProcedure("\$init"), counter)
        assertEquals(10, counter.target.getField(type["count"]).value)
        assertEquals(18, counter.target.getField(type["doubleCount"]).value)

        val counter2 = vm.allocateRecord(type)
        vm.execute(module.getProcedure { it.id == "\$init" && it.parameters.size == 2}!!, counter2, vm.getValue(5))
        assertEquals(14, counter2.target.getField(type["count"]).value)
        assertEquals(18, counter.target.getField(type["doubleCount"]).value)
    }

    @Test
    fun `With Default Constructor`() {
        val src = """
            class Counter {
                private int count = 10;
                
                public void inc() {
                    count++;
                }
                
                public void dec() {
                    count--;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()
        val counter = vm.allocateRecord(type)

        vm.execute(module.getProcedure("\$init"), counter)
        assertEquals(10, counter.target.getField(type["count"]).value)
    }
}
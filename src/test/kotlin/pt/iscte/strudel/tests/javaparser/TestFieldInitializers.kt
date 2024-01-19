package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestFieldInitializers {

    private fun test(src: String) {
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()
        val counter = vm.allocateRecord(type)

        vm.execute(module.getProcedure("\$init"), counter)

        assertEquals(
            10,
            counter.target.getField(type["count"]).value
        )
    }

    @Test
    fun `With Explicit Constructor`() {
        test("""
            public class Counter {
                private int count = 9;
                
                public Counter() {
                    count++;
                }
                
                public void inc() {
                    count++;
                }
                
                public void dec() {
                    count--;
                }
            }
        """.trimIndent())
    }

    @Test
    fun `With Default Constructor`() {
        test("""
            public class Counter {
                private int count = 10;
                
                public void inc() {
                    count++;
                }
                
                public void dec() {
                    count--;
                }
            }
        """.trimIndent())
    }
}
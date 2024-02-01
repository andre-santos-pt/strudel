package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestDependentFieldDeclarations {

    @Test
    fun `multi var`() {
        val src = """
            public class Counter {
                private int x = 10, y = x*3;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()
        val counter = vm.allocateRecord(type)

        vm.execute(module.getProcedure("\$init"), counter)
        assertEquals(10, counter.target.getField(type["x"]).value)
        assertEquals(30, counter.target.getField(type["y"]).value)
    }

    @Test
    fun `single vars`() {
        val src = """
            public class Counter {
                private int x = 10;
                private int y = x*3;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()
        val counter = vm.allocateRecord(type)

        vm.execute(module.getProcedure("\$init"), counter)
        assertEquals(10, counter.target.getField(type["x"]).value)
        assertEquals(30, counter.target.getField(type["y"]).value)
    }

    @Test
    fun `var default val`() {
        val src = """
            public class Counter {
                private int x;
                 private int y = x+3;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Counter")

        println(module)

        val vm = IVirtualMachine.create()
        val counter = vm.allocateRecord(type)

        vm.execute(module.getProcedure("\$init"), counter)
        assertEquals(0, counter.target.getField(type["x"]).value)
        assertEquals(3, counter.target.getField(type["y"]).value)
    }
}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class TestConditionalJava {
    val code = """    
        class Test {
            static int caseTrue(int a) {
                return a;
            }
            
            static int caseFalse(int a) {
                return a;
            }
            
            static int max(int a, int b) {
                return a > b ? caseTrue(a) : caseFalse(b);
            }
        }
    """.trimIndent()

    @Test
    fun `test single path execution`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        vm.addListener(object : IVirtualMachine.IListener {
            var i = 0
            override fun procedureCall(procedure: IProcedure, args: List<IValue>, caller: IProcedure?) {
                if(procedure.id != "max") {
                    if (i == 0)
                        assertEquals("caseFalse", procedure.id)
                    else if (i == 1)
                        assertEquals("caseTrue", procedure.id)
                    else
                        throw AssertionError("unexpected call")
                    i++
                }
            }
        })
        val a = vm.getValue(3)
        val b = vm.getValue(5)
        val ret1 = vm.execute(model.procedures.find { it.id == "max" }!! as IProcedure, a, b)
        assertEquals(b.toInt(), ret1?.toInt())

        val ret2 = vm.execute(model.procedures.find { it.id == "max" }!! as IProcedure, b, a)
        assertEquals(b.toInt(), ret2?.toInt())
    }


    val code2 = """    
        class Test {
            static int test(int n) {
                return n + (n % 2 == 0 ? 1 : 2);
            }
        }
    """.trimIndent()

    @Test
    fun `test combined expression`() {
        val model = Java2Strudel().load(code2)
        val vm = IVirtualMachine.create()

        val a = vm.getValue(2)
        val ret1 = vm.execute(model.procedures.find { it.id == "test" }!!  as IProcedure, a)
        assertEquals(vm.getValue(3).toInt(), ret1?.toInt())

        val b = vm.getValue(3)
        val ret2 = vm.execute(model.procedures.find { it.id == "test" }!! as IProcedure, b)
        assertEquals(vm.getValue(5).toInt(), ret2?.toInt())

    }
}
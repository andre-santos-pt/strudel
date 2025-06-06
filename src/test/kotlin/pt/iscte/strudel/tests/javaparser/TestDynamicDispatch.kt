package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IVirtualMachine

class TestDynamicDispatch {
    val code = """
        interface Strategy {
            int strategy(int n);
        }
        
        class S1 implements Strategy {
            public int strategy(int n) {
                return n+1;
            }
        }
        
        class S2 implements Strategy {
            public int strategy(int n) {
                return n-1;
            }
        }
        
        class Test {
            static int test1() {
                Strategy s = new S1();
                return apply(1, s);
            }
            
            static int test2() {
                Strategy s = new S2();
                return apply(1, s);
            }
            
            static int apply(int n, Strategy s) {
                int r = s.strategy(n);
                return r;
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()

        val ret1 = vm.execute(model.procedures.find { it.id == "test1" }!! as IProcedure)
        assertEquals(vm.getValue(2).toInt(), ret1?.toInt())

        val ret2 = vm.execute(model.procedures.find { it.id == "test2" }!! as IProcedure)
        assertEquals(vm.getValue(0).toInt(), ret2?.toInt())
    }
}
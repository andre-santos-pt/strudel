package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.*

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

        val ret1 = vm.execute(model.procedures.find { it.id == "test1" }!!)
        assertEquals(vm.getValue(2).toInt(), ret1?.toInt())

        val ret2 = vm.execute(model.procedures.find { it.id == "test2" }!!)
        assertEquals(vm.getValue(0).toInt(), ret2?.toInt())
    }
}
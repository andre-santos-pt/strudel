package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals

class TestSameIdentifierVariables {

    @Test
    fun `test for`() {
        val src = """
            class ExtremelyUsefulStaticMethods {
                public static int get() {
                    int result = 0;
                    
                    for (int i = 0; i < 10; i++) {
                        result += 2;
                    }
                    
                    for (int i = 0; i < 10; i++) {
                        result -= 1;
                    }
                    
                    return result;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")


        val ret = vm.execute(get)?.value
        assertEquals(10, ret)
    }

    @Test
    fun `test inner var`() {
        val src = """
            class ExtremelyUsefulStaticMethods {
                public static int get(int n) {
                    if(n % 2 == 0) {
                        int tmp = n / 2;
                        return tmp;
                    } else {
                        int tmp = n * 2;
                        return tmp;
                    }
                        
              
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val vm = IVirtualMachine.create()
        val get = module.getProcedure("get")

        val ret1 = vm.execute(get, vm.getValue(4))?.value
        assertEquals(2, ret1)

        val ret2 = vm.execute(get, vm.getValue(5))?.value
        assertEquals(10, ret2)
    }
}
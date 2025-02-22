package pt.iscte.strudel.tests.javaparser

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import kotlin.test.Test
import kotlin.test.fail


class TestVarsWithSameId {


    @Test
    fun testWhile() {
        val module = Java2Strudel().load(
            """
            class Test {
              static void main() {
                {
			        int i = 0;
			        while((i < 10)) {
				        i = (i + 1);
			        }
		        }
		        {
			        int i = 0;
			        while((i < 20)) {
				        i = (i + 2);
			        }
		        }
             }
            }
    """.trimIndent()
        )

        val vm = IVirtualMachine.create(loopIterationMaximum = 10)
        try {
            vm.execute(module.getProcedure("main"))
        } catch (e: RuntimeError) {
            fail("should not reach limit")
        }
    }

    @Test
    fun testFor() {
        val module = Java2Strudel().load(
            """
            class Test {
                static void main() {
                    for(int i = 0; i < 10; i = i + 1) {
                    }
        
                    for(int i = 0; i < 10; i = i + 2) {
                    }
                }
            }
    """.trimIndent()
        )

        val vm = IVirtualMachine.create(loopIterationMaximum = 10)
        try {
            vm.execute(module.getProcedure("main"))
        } catch (e: RuntimeError) {
            fail("should not reach limit")
        }
    }
}
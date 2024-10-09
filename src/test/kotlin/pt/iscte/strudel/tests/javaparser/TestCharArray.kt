package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.CHAR
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError

class TestCharArray {
    val code = """    
        class Test {
            static void replace(char[] letters, char find, char replace) {
                for(int i = 0; i < letters.length; i++)
                    if(letters[i] == find)
                        letters[i] = replace;
            }
       }
       
    """.trimIndent()
    @Test
    fun test() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        val letters = vm.allocateArrayOf(CHAR, 'a','b','c')
        val r = try {
            vm.execute(module.getProcedure("replace"), letters, vm.getValue('b'), vm.getValue('z'))
        }
        catch (e: RuntimeError) {
            println(e.sourceElement)
        }
        println(module)
        println(letters)
    }
}
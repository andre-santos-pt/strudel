package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

class TestDuplicateLocalVarId {

    val code = """    
        class Test {
       static int gcd (int a, int b){
           if (a < b){
               int temp = a;
               a = b;
               b = temp;
           } 
           while (b != 0){
                   int temp = b;
                   b = a % b;
                   a = temp;
               }
           return a;
       }
       }
       
    """.trimIndent()
    @Test
    fun test() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        val r = vm.execute(module.getProcedure("gcd"), vm.getValue(30), vm.getValue(25))

        println(module)
        println(r)

    }
}
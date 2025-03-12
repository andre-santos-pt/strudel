package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

class TestIntDoubleEquality {

    val code = """  
          class Test {
        static boolean isMultiple(double a, int b){
    int control = b;
    double transport = a;
    boolean result = false;
    while (control >= 0){
        transport = transport - b;
        control = control - 1;
    }
    if (transport == 0){
        result = true;
    }
    return result;
}
 }
    """.trimIndent()
    @Test
    fun test() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)
        val r = vm.execute(module.getProcedure("isMultiple"), vm.getValue(30.0),vm.getValue(5))
    println(r)
        assertTrue(r?.isTrue == true)
    }
}
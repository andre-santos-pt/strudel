package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

class TestSqrtNewton {

    val code = """  
          class Test {
         static double dist(int x1, int y1, int x2, int y2) {
     int a = (x2 - x1) * (x2 - x1);
     int b = (y2 - y1) * (y2 - y1);
     int c = a + b;
     double epsilon = 1.0e-15; // relative error tolerance
     double t = c; // estimate of the square root of c

     // repeatedly apply Newton update step until desired precision is achieved
     while (Math.abs(t - c / t) > .0000001 * t) {
         t = (c / t + t) / 2.0;
     }
     return t;
 }
 }
    """.trimIndent()
    @Test
    fun testNewton() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)
        val r = vm.execute(module.getProcedure("dist"), vm.getValue(1),vm.getValue(2),vm.getValue(5),vm.getValue(4))
    println(r)
    }
}
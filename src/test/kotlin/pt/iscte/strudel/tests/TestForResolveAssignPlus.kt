package pt.iscte.strudel.tests

import pt.iscte.strudel.parsing.java.Java2Strudel
import kotlin.test.Test

class TestForResolveAssignPlus {

    val src = """
        public class Test {
        static int sumEvenBetween (int min, int max){
    int sum = 0;
    if (min % 2 != 0){
        min++;
    }
    for (int i = min; i <=max; i+=2){
        sum += i;
    
    }
    return sum;
}
        }
    """.trimIndent()
    @Test
    fun test() {
        val m = Java2Strudel().load(src, "Test")
        println(m)
    }

}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel

class TestArrayInitializersNDim {
    val code = """
        class Test {
            static void main() {
                int[] v0 = {};
                int[] v1 = new int[] {1,2,3};
                int[] v2 = {1,2,3};
                int[][] m = {
                    {1,2,3},
                    {4,5,6},
                    {7,8,9}
                };
                int[][] empty = {};
                int[][][][] n4 = new int[30][20][10][5];
               
            }
        }
    """.trimIndent()


    @Test
    fun test() {
        val model = Java2Strudel(checkJavaCompilation = false).load(code)
        println(model)
    }
}
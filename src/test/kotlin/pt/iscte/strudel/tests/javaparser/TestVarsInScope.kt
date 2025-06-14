package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.util.find
import org.junit.jupiter.api.Assertions.assertEquals

class TestVarsInScope {

    @Test
    fun test() {
        val src = """
            class Test {
                public static void bubbleSort(int[] arr) {
                    int n = arr.length;
                    boolean swapped;

                    for (int i = 0; i < n - 1; i++) {
                        swapped = false;
                        for (int j = 0; j < n - 1 - i; j++) {
                            if (arr[j] > arr[j + 1]) {
                                int temp = arr[j];
                                arr[j] = arr[j + 1];
                                arr[j + 1] = temp;
                                swapped = true;
                            }
                        }
                        if (!swapped) break;
                    }
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val procedure = module.getProcedure("bubbleSort") as IProcedure
        val innerLoop = procedure.find(ILoop::class,1)
        var varsInner = innerLoop.block.variablesInScope().map { it.id }
        assertEquals(listOf("j","i","n","swapped","arr"), varsInner)

        val outerLoop = procedure.find(ILoop::class,0)
        var varsOuter = outerLoop.block.variablesInScope().map { it.id }
        assertEquals(listOf("i","n","swapped","arr"), varsOuter)

        val procBody = procedure.block
        var varsBody = procBody.variablesInScope().map { it.id }
        assertEquals(listOf("n","swapped","arr"), varsBody)
    }
}
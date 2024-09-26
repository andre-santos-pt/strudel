package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.impl.ArrayElementAssignment
import pt.iscte.strudel.model.impl.RecordFieldAssignment
import pt.iscte.strudel.parsing.java.LoadingError
import kotlin.test.assertIs

class TestUnaryExpressionReplace {
    @Test
    fun test() {
        val src = """
            public class ArrayList {
                int[] array = {1,2,3};
                int next = 0;
                
                void add(int n) {
                  array[next++] = n;
                }
                
                void removeLast() {
                  array[--next] = 0;
                }
                
                void addNext(int n) {
                  array[next++] = ++n;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val body = module.get("add").block
        assertIs<ArrayElementAssignment>(body.children[0])
        assertIs<RecordFieldAssignment>(body.children[1])
    }

    @Test
    fun testIf() {
        val src = """
            public class ArrayList {
                int[] array = {1,2,3};
                int next = 0;
                
                void add(int n) {
                  if(array[next++] == 0)
                    array[next] = n;
                }
            }
        """.trimIndent()
        assertThrows<LoadingError> {
            Java2Strudel().load(src)
        }
    }

    @Test
    fun testWithinIf() {
        val src = """
            public class Test {
                int inc(int n) {
                    return n + 1;
                }
                
                int test(int n) {
                    if (true)
                        return inc(--n);
                    else
                        return inc(--n);
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)
    }

    @Test
    fun testUnsupport() {
        val src = """
            public class ArrayList {
                int[] array = {1,2,3};
                int next = 0;
                
                void addStrange() {
                  array[next++] = next;
                }
            }
        """.trimIndent()
        assertThrows<LoadingError> {
            Java2Strudel().load(src)
        }
    }

    @Test
    fun testFor() {
        val src = """
            public class ArrayList {
                int[] list = {1,2,3};
            
                int test() {
                    int sum = 0;
                    for (int i = 0; i < list.length; i++)
                        sum = sum + list[i];
                    return sum;
                }
            }
        """.trimIndent()
        val model = Java2Strudel().load(src)
        println(model)
    }
}
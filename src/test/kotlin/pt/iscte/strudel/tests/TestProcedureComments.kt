package pt.iscte.strudel.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*

class TestProcedureComments {

    @Test
    fun `Comment set through DSL`() {
        val procedure = Procedure(INT, "sum", "This function sums all the values of an integer array.") {
            val a = Param(array(INT), "a")

            val sum = Var(INT, "sum")
            val i = Var(INT, "i")
            Assign(sum, 0)
            Assign(i, 0)

            While(exp(i) smaller a.length()) {
                Assign(sum, sum + a[exp(i)])
                Assign(i, i + 1)
            }

            Return(exp(sum))
        }

        assertEquals("This function sums all the values of an integer array.", procedure.comment)
    }

    @Test
    fun `Parsed line comment`() {
        val src = """
            public class IntArraySum {
                
                // This function sums all the values of an integer array.
                public static int sum(int[] a) {
                    int sum = 0;
                    for (int i = 0; i < a.length; i++) {
                        sum += a[i];
                    }
                    return sum;
                }
            }
        """.trimIndent()

        val procedure = Java2Strudel().load(src).getProcedure("sum")

        assertEquals("This function sums all the values of an integer array.", procedure.comment)
    }

    @Test
    fun `Parsed Javadoc comment`() {
        val src = """
            public class IntArraySum {
                
                /**
                This is a Javadoc comment.
                This function sums all the values of an integer array.
                */
                public static int sum(int[] a) {
                    int sum = 0;
                    for (int i = 0; i < a.length; i++) {
                        sum += a[i];
                    }
                    return sum;
                }
            }
        """.trimIndent()

        val procedure = Java2Strudel().load(src).getProcedure("sum")

        // The """ strings are being weird and converting tabs to 4 individual spaces
        assertEquals(
            "This is a Javadoc comment.\n    This function sums all the values of an integer array.",
            procedure.comment
        )
    }
}
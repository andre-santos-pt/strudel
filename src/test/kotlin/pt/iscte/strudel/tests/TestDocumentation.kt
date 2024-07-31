package pt.iscte.strudel.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.find

class TestDocumentation {

    @Test
    fun `Procedure comment set through DSL`() {
        val procedure = Procedure(INT, "sum") {
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
        }.apply {
            documentation = "This function sums all the values of an integer array."
        }

        assertEquals("This function sums all the values of an integer array.", procedure.documentation)
    }

    @Test
    fun `Parsed procedure line comment`() {
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

        assertEquals("This function sums all the values of an integer array.", procedure.documentation)
    }

    @Test
    fun `Parsed procedure Javadoc comment`() {
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
            procedure.documentation
        )
    }

    @Test
    fun `Parsed record line comment`() {
        val src = """
            // This is a simple class with no procedures.
            public class CompletelyEmpty {

            }
        """.trimIndent()

        val record = Java2Strudel().load(src).getRecordType("CompletelyEmpty")

        assertEquals("This is a simple class with no procedures.", record.documentation)
    }

    @Test
    fun `Parsed statement line comment`() {
        val src = """
            public class HelloWorld {
            
                public static int hello() {
                    System.out.println("Hello there! This function will return the integer value 42.");
                    return 42; // Returns the int 42. :)
                }
            }
        """.trimIndent()

        val procedure = Java2Strudel().load(src).getProcedure("hello")
        val returnStmt = procedure.find(IReturn::class, 0)

        assertEquals("Returns the int 42. :)", returnStmt.documentation)
    }
}
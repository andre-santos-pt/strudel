package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.LoadingError
import pt.iscte.strudel.parsing.java.LoadingErrorType
import kotlin.test.assertEquals

class TestLoadingErrors {

    @Test
    fun testCompilation() {
        val src = """
            class Silly {
                public;
            }
        """.trimIndent()
        val ex = assertThrows<LoadingError> { Java2Strudel().load(src) }
        println(ex.messages.joinToString(System.lineSeparator()))

        assertEquals(ex.type, LoadingErrorType.JAVA_COMPILATION)

        assertEquals(1, ex.messages.size)

        val (_, location) = ex.messages.first()

        assertEquals(2, location.startLine)
        assertEquals(2, location.endLine)

        assertEquals(11, location.startColumn)
        assertEquals(11, location.endColumn)

        assertEquals(0, location.length)
    }

    @Test
    fun testUnsupportedDeclaration() {
        val src = """
            enum ProblemType {
                BAD,
                WORSE,
                WORST;
            }
        """.trimIndent()
        val ex = assertThrows<LoadingError> { Java2Strudel().load(src) }
        println(ex.messages.joinToString(System.lineSeparator()))

        assertEquals(ex.type, LoadingErrorType.UNSUPPORTED)

        assertEquals(1, ex.messages.size)

        val (message, location) = ex.messages.first()

        assertEquals("unsupported enum declarations", message)

        assertEquals(1, location.startLine)
        assertEquals(1, location.endLine)

        assertEquals(6, location.startColumn)
        assertEquals(16, location.endColumn)

        assertEquals(11, location.length)
    }

    @Test
    fun testUnsupportedModifiers() {
        val src = """
            class Problematic {
                protected void foo() {
                    System.out.println("bar");
                }
            }
        """.trimIndent()
        val ex = assertThrows<LoadingError> { Java2Strudel().load(src) }
        println(ex.messages.joinToString(System.lineSeparator()))

        assertEquals(ex.type, LoadingErrorType.UNSUPPORTED)

        assertEquals(1, ex.messages.size)

        val (message, location) = ex.messages.first()
        assertEquals("unsupported modifier protected", message)
        assertEquals(2, location.startLine)
        assertEquals(2, location.endLine)
        assertEquals(5, location.startColumn)
        assertEquals(13, location.endColumn)
        assertEquals(9, location.length)
    }

    @Test
    fun testExtendsKeyword() {
        val src = """
            class Graphics {
                class Shape { }
                class Ball extends Shape { }
            }
        """.trimIndent()
        val ex = assertThrows<LoadingError> { Java2Strudel().load(src) }
        println(ex.messages.joinToString(System.lineSeparator()))

        assertEquals(ex.type, LoadingErrorType.UNSUPPORTED)

        assertEquals(1, ex.messages.size)

        val (message, location) = ex.messages.first()
        assertEquals("unsupported extends keyword", message)
        assertEquals(3, location.startLine)
        assertEquals(3, location.endLine)
        assertEquals(24, location.startColumn)
        assertEquals(28, location.endColumn)
        assertEquals(5, location.length)
    }

    @Test
    fun testNewArrayCompilationErrorLocation() {
        val src = """
            class Test {
                static int foo(int[] a) {
                    int[] b = int[a.length];
                    return b.length;
                }
            }
        """.trimIndent()
        val ex = assertThrows<LoadingError> { Java2Strudel().load(src) }
        println(ex.messages.joinToString(System.lineSeparator()))

        assertEquals(ex.type, LoadingErrorType.JAVA_COMPILATION)
    }
}
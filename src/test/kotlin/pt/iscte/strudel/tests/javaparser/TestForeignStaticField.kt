package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.strudel.parsing.java.Java2Strudel

class TestForeignStaticField {

    val src = """
        import java.util.Scanner;
        
        class Input {
            public static void init() {
                Scanner s = new Scanner(System.in);
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        assertDoesNotThrow { Java2Strudel().load(src) }
    }
}
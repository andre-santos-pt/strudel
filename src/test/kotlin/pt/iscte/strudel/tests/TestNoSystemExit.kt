package pt.iscte.strudel.tests

import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.LoadingError
import kotlin.test.Test

class TestNoSystemExit {

    val src = """
        public class Test {
            static void main() {
                System.exit(0);
            }
        }
    """.trimIndent()
    @Test
    fun test() {
        assertThrows<LoadingError> {
            Java2Strudel().load(src, "Test")
        }
    }

}
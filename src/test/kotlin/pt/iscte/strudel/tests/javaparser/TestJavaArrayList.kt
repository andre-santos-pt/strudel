package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel

class TestJavaArrayList {

    @Test
    fun test() {
        val src = """
            import java.util.List;
            import java.util.ArrayList;
            
            class HelloWorld {
                private List<Integer> values = new ArrayList<>();
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        println(module)

        val recordType = module.getRecordType("HelloWorld")

        val field = recordType.getField("values")
        assertNotNull(field)
        assertEquals("java.util.List", field!!.type.id)
    }
}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.HostRecordType
import pt.iscte.strudel.model.IReferenceType
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TestGenericType {
    @Test
    fun test() {
        val src = """
            public class Wrapper<T> {
                private T value;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        val field = module.getType("Wrapper").asRecordType.getField("value")
        assertNotNull(field)
        assertIs<IReferenceType>(field.type)
        assertIs<HostRecordType>((field.type as IReferenceType).target)
        assertEquals("java.lang.Object", field.type.id)
    }
}
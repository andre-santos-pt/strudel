package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.HostRecordType
import pt.iscte.strudel.model.IReferenceType

class TestGenericType {
    @Test
    fun test() {
        val src = """
            class Wrapper<T> {
                private T value;
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)

        val field = module.getType("Wrapper").asRecordType.getField("value")
        assertNotNull(field)
        assertTrue(field?.type is IReferenceType)
        assertTrue((field?.type as IReferenceType).target is HostRecordType)
        assertEquals("java.lang.Object", field.type.id)
    }
}
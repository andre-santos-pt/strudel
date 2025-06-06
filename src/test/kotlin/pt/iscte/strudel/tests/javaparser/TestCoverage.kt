package pt.iscte.strudel.tests.javaparser

import com.github.javaparser.ast.Node
import org.junit.jupiter.api.Test
import pt.iscte.strudel.examples.CodeCoverageExample
import pt.iscte.strudel.examples.codeCoverage
import pt.iscte.strudel.parsing.java.Java2Strudel
import org.junit.jupiter.api.Assertions.assertEquals

class TestCoverage {
    @Test
    fun test() {
        val module = Java2Strudel().load(CodeCoverageExample.example)
        val lines = codeCoverage(module, "main").map {
            (it.key.getProperty("JP") as Node).toString() to it.value
        }.toMap()

        assertEquals(1, lines["int i = 1;"])
        assertEquals(1, lines["int even = 0;"])
        assertEquals(1, lines["int odd = 0;"])
        assertEquals(10, lines["i++;"])
        assertEquals(5, lines["even++;"])
        assertEquals(5, lines["odd++;"])
        assertEquals(5, lines["return true;"])
        assertEquals(5, lines["return false;"])
    }
}
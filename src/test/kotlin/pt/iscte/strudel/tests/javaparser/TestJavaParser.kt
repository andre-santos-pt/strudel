package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.proceduresExcludingConstructors
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.impl.VirtualMachine
import kotlin.io.path.Path
import kotlin.test.assertTrue


class TestJavaParser {

    @Test
    fun testFile() {
        val m = Java2Strudel().load(Path("src", "test", "java","BinarySearch.java").toFile())
        val vm = VirtualMachine()
        val array = vm.allocateArrayOf(INT, 1, 3, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22)
        val r = vm.execute(m.proceduresExcludingConstructors.first()  as IProcedure, array, vm.getValue(10))
        assertTrue(r!!.toBoolean())
    }

}
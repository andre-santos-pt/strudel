package pt.iscte.strudel.tests.javaparser

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSortingAlgorithms {
    @Test
    fun test() {
        val case = listOf(4, 3, 1, 6, 10, 8, 20, 5, 6, 90, 34, 65, 29, 59, 39, 19,3, 6, 7, 12, 17)
        val expected = listOf(1, 3, 3, 4, 5, 6, 6, 6, 7, 8, 10, 12, 17, 19, 20, 29, 34, 39, 59, 65, 90)

        println(System.getProperty("user.dir"))
        val vm = IVirtualMachine.create()
        val model = Java2Strudel().load(Path("src", "test", "java","Sorting.java").toFile())
        // TODO missing InsertionSort -- short-circuit  &&
        model.procedures.filter { it.id!!.endsWith("Sort") }.forEach {
            val array = vm.allocateArrayOf(INT, *case.toTypedArray())
            vm.execute(it  as IProcedure, array)
            expected.forEachIndexed { index, e ->
                assertEquals(e, array.target.getElement(index).toInt())
            }
        }
    }
}
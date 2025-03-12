package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import kotlin.test.assertTrue

class TestBinarySearch : BaseTest({
    Procedure(BOOLEAN, "binarySearch") {
        val array = Param(INT.array().reference())
        val e = Param(INT)
        val l = Var(INT, 0)
        val r = Var(INT, array.length() - 1)
        While(l smallerEq r) {
            val m = Var(INT, l + (r - l) / 2)
            If(array[m] equal e) {
                Return(True)
            }
            If(array[m] smaller e) {
                Assign(l, m + 1)
            }.Else {
                Assign(r, m - 1)
            }
        }
        Return(False)
    }
}
) {

    @Test
    fun test() {
        val case = vm.allocateArrayOf(INT, 1, 3, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22)
        assertTrue(vm.execute(procedure, case, vm.getValue(7))!!.value == true)
        assertTrue(vm.execute(procedure, case, vm.getValue(2))!!.value == false)
        println(procedure)
    }

}




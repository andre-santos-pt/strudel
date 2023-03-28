package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.CHAR
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*

class TestChars : pt.iscte.strudel.tests.BaseTest({
    Procedure(CHAR, "next") {
        val c = Param(CHAR, "a")
        If(c.expression() equal character('z')) {
            Return(character('a'))
        }.Else {
            Return(c + 1)
        }
    }
}){

    @Test
    fun test() {
        val a = vm.allocateArrayOf(INT, 2, 4, 6, 8)
        vm.execute(procedure, a, vm.getValue('b'))

    }

}
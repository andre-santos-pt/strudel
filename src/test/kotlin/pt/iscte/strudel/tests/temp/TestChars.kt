package pt.iscte.strudel.tests.temp

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.CHAR
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.tests.BaseTest
import kotlin.test.assertEquals

class TestChars : BaseTest({
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
        val res = vm.execute(procedure, vm.getValue('b'))
        assertEquals('c', res!!.toChar())
    }

}
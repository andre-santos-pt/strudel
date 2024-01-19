package pt.iscte.strudel.tests.temp

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.ANY
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.tests.BaseTest
import pt.iscte.strudel.tests.approxEqual
import kotlin.test.assertTrue

class TestUnboundType : BaseTest({
    Procedure(ANY, "sum") {
        val a = Param(ANY, "a")
        val s = Var(ANY, "s")
        Assign(s, 0.0)
        val i = Var(ANY, "i")
        Assign(i, 0)
        While(i.expression() smaller a.length()) {
            Assign(s, s + a[i.expression()] )
            Assign(i, i + 1)
        }
        Return(s)
    }
}){

    @Test
    fun test() {
        val a = vm.allocateArrayOf(DOUBLE, 2.0, 4.1, 6.2, 8.4)
        val ret = vm.execute(procedure, a)
       assertTrue(ret!!.toDouble().approxEqual(20.7))
    }
}
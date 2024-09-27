package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.Param
import pt.iscte.strudel.model.dsl.Procedure
import pt.iscte.strudel.model.dsl.Return
import pt.iscte.strudel.vm.impl.IForeignProcedure
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class TestBuiltinRound: pt.iscte.strudel.tests.BaseTest({
  Procedure(INT, "roundInt") {
      val d = Param(DOUBLE, "n")
      Return(MathRound.expression(d.expression()))
  }
}, listOf(MathRound), javaCompile = false) {

    @Test
    fun test() {
        println(procedure.createCFG())
        val r1 = vm.execute(procedure, vm.getValue(2.4))
        assertEquals(2, r1!!.toInt())

        val r2 = vm.execute(procedure, vm.getValue(2.7))
        assertEquals(3, r2!!.toInt())
    }
}

val MathRound = IForeignProcedure.create(
    "Math",
    "round",
    INT,
    listOf(DOUBLE)
) { m, args ->
    m.getValue(args[0].toDouble().roundToInt())
}
package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestLoopEnd : BaseTest({
  Procedure(INT.array().reference(), "naturals") {
    val n = Param(INT, "n")
    val a = Var(INT.array().reference(), "a")
    Assign(a, INT.array().heapAllocation(n.expression()))
    val i = Var(INT, "i")
    Assign(i, 0)
    While(i.expression() smaller n.expression()) {
      ArraySet(a, i.expression(), i + 1)
      Assign(i, i + 1)
    }
    Return(a)
  }
}) {


  @Test
  fun empty() {
    vm.addListener(object : IVirtualMachine.IListener {
      var flag = false
      override fun loopEnd(loop: ILoop) {
        assertFalse(flag)
        flag = true
      }
    })
    vm.execute(procedure, vm.getValue(0))
  }

  @Test
  fun notEmpty() {
    var count = 0
    vm.addListener(object : IVirtualMachine.IListener {
      var flag = false
      override fun loopEnd(loop: ILoop) {
        assertFalse(flag)
        flag = true
      }

      override fun loopIteration(loop: ILoop) {
        count++
      }
    })
    vm.execute(procedure, vm.getValue(5))
    assertEquals(5, count)
  }

}

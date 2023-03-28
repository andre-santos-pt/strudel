package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.JavaType
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.IForeignProcedure
import pt.iscte.strudel.vm.impl.Value
import java.util.*
import kotlin.test.assertTrue

class TestJDKLinkedList : pt.iscte.strudel.tests.BaseTest({
    Procedure(linkedList, "testlist") {
        val list = Var(linkedList, "list")
        Assign(list, LListCreate.expression())
        Call(LListAdd, list.expression(), INT.literal(1))
        Call(LListAdd, list.expression(), INT.literal(2))
        Call(LListAdd, list.expression(), INT.literal(3))
        Return(list.expression())
    }
}, listOf(LListCreate, LListAdd)) {

    @Test
    fun test() {
        procedure.createCFG().display()
        val r = vm.execute(procedure)
        val list = r!!.value as List<*>
        assertTrue(3 == list.size)
        (1..3).forEachIndexed() { i, e -> assertTrue((list[i] as IValue).value == e)}
    }
}

val objectType = JavaType(Object::class.java)

val linkedList = JavaType(LinkedList::class.java)

val LListCreate = IForeignProcedure.create("LinkedList", "create", linkedList, emptyList()) { m, args ->
    Value(linkedList, LinkedList<Any>())
}

val LListAdd = IForeignProcedure.create("LinkedList", "add", VOID, listOf(linkedList, objectType)) { m, args ->
    (args[0].value as MutableList<Any>).add(args[1])
    pt.iscte.strudel.vm.VOID
}
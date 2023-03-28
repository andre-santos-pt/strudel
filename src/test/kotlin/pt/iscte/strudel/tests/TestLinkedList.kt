package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.NULL_LITERAL
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.vm.addAllocationTracker
import kotlin.test.assertEquals

class TestLinkedList : pt.iscte.strudel.tests.BaseTest({

    val nodeType = Record("Node") {
        Field(INT, "element")
        Field(this, "next")
    }

    val listType = Record("SingleLinkedList") {
        Field(nodeType, "first")
        Field(nodeType, "last")
    }

    Procedure(BOOLEAN, "isEmpty") {
        val list = Param(listType, "list")
        Return(RelationalOperator.EQUAL.on(list.field(listType.fields[0]), NULL_LITERAL))
    }

    Procedure(INT, "size") {
        val list = Param(listType, "list")
        val n = Var(nodeType, "n", list["first"])
        val c = Var(INT, "c", 0)
        While(n.expression() notEqual NULL_LITERAL) {
            Assign(c, c + 1)
            Assign(n, n.field(nodeType["next"]))
        }
        Return(c)
    }

    Procedure(VOID, "add") {
        val list = Param(listType, "list")
        val e = Param(INT, "e")
        val n = Var(nodeType, "n", nodeType.heapAllocation())
        FieldSet(n.expression(), nodeType.fields[0], e.expression())
        If(list.field(listType.fields[0]) equal NULL_LITERAL) {
            FieldSet(list.expression(), listType.fields[0], n.expression())
            FieldSet(list.expression(), listType.fields[1], n.expression())
        }.Else {
            val i = Var(nodeType, "i", list["first"])
            While(i["next"] notEqual NULL_LITERAL) {
                Assign(i, i["next"])
            }
            FieldSet(i, nodeType["next"], n.expression())
            FieldSet(list, listType["last"], n.expression())
        }

    }
}){

    @Test
    fun test() {
        val objtracker = vm.addAllocationTracker()

        val list = vm.allocateRecord(module.getRecordType("SingleLinkedList"))

        assertEquals(true, vm.execute(module["isEmpty"], list)!!.toBoolean())
        assertEquals(0, vm.execute(module["size"], list)!!.toInt())

        vm.execute(module["add"], list, vm.getValue(2))
        vm.execute(module["add"], list, vm.getValue(3))
        vm.execute(module["add"], list, vm.getValue(4))
        assertEquals(3, vm.execute(module["size"], list)!!.toInt())
        assertEquals(false, vm.execute(module["isEmpty"], list)!!.toBoolean())

        assertEquals(4, objtracker.allAllocations().size)
        assertEquals(1, objtracker[module.getRecordType("SingleLinkedList")].size)
        assertEquals(3, objtracker[module.getRecordType("Node")].size)
    }


}
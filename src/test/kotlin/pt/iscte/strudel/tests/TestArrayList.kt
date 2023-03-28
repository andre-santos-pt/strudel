package pt.iscte.strudel.tests
import kotlin.test.Test
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import kotlin.test.assertEquals

class TestArrayList : pt.iscte.strudel.tests.BaseTest({

    val listType = Record("IntArrayList") {
        Field(array(INT), "elements")
        Field(INT, "count")
    }

    Procedure(VOID, "init") {
        val list = Param(listType, "list")
        FieldSet(list.expression(),listType.fields[0], INT.array().heapAllocation(
            lit(10)
        ))
        FieldSet(list.expression(), listType.fields[1], lit(0))
    }

    Procedure(INT, "size") {
        val list = Param(listType, "list")
        Return(list.field(listType.fields[1]))
    }

    Procedure(VOID, "add") {
        val list = Param(listType, "list")
        val n = Param(INT, "n")
        ArraySet(list.field(listType.fields[0]), list.expression().field(listType.fields[1]), n.expression())
        FieldSet(list.expression(), listType.fields[1], list.expression().field(listType.fields[1]) + 1)
    }
}){


    @Test
    fun test() {
        val list = vm.allocateRecord(module.recordTypes[1])
        vm.execute(module.procedures[0], list)
        vm.execute(module.procedures[2], list, vm.getValue(2))
        vm.execute(module.procedures[2], list, vm.getValue(3))
        val r = vm.execute(module.procedures[1], list)
        assertEquals(2, vm.execute(module.procedures[1], list)!!.toInt())
        //list.getField()
        //Assert.assertTrue(vm.error is ArrayIndexError && (vm.error as ArrayIndexError).invalidIndex == 6)
    }


}
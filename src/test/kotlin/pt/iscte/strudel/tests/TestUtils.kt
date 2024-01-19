package pt.iscte.strudel.tests

import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IReferenceType
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import kotlin.math.abs
import kotlin.test.assertEquals


fun IModule.procedure(id: String) =  procedures.find{ it.id == id }!!

fun IValue.checkArrayContent(vararg values: Int) {
    require(this is IReference<*> && this.target is IArray)
    (this.target as IArray).checkContent(*values)
}

fun IReferenceType.checkArrayContent(vararg values: Int) =
    (target as IArray).checkContent(*values)

fun IArray.checkContent(vararg values: Int) {
    assertEquals(length, values.size)
    values.forEachIndexed() {
            i, e ->
        assertEquals(getElement(i).toInt(), e, "expected: ${values.toList()}, found: $this")
    }
}

fun Double.approxEqual(d: Double) = abs(this - d) < .000000001
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.tests.procedure
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertEquals
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IExpressionHolder
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue

class TestArray3dInitializer {
    val code = """
        class Test {
            static int[][][] main() {
                int[][][] img = new int[10][5][3];
                return img;
            }
        }
    """.trimIndent()


    @Test
    fun test() {
        val model = Java2Strudel(checkJavaCompilation = false).load(code)
        println(model)
        val vm = IVirtualMachine.create(availableMemory = 1000000)
        val r = vm.execute(model.procedure("main"))
        assertEquals(10,(r as IReference<IArray>).target.length)
        //assertEquals(model.getReferenceType(model.getArrayType(INT, 3)), r.type) // TODO without string
        assertEquals("int[][][]", r.type.toString())
        assertEquals(5, ((r.target.getElement(0) as IReference<IArray>).target.length))
        assertEquals("int[][]", r.target.getElement(0).type.toString())
        assertEquals(3, ((r.target.getElement(0) as IReference<IArray>).target.getElement(0) as IReference<IArray>).target.length)
        assertEquals("int[]", ((r.target.getElement(0) as IReference<IArray>).target.getElement(0) as IReference<IArray>).type.toString())
    }
}
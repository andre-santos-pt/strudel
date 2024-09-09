package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.hasThisParameter
import pt.iscte.strudel.vm.ExceptionError
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestJavaRecord {

    @Test
    fun testPoint() {
        val src = """
            record Point(int x, int y) { }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Point")

        println(module)

        val vm = IVirtualMachine.create()
        val point = vm.allocateRecord(type)

        val constructor = module.getProcedure("\$init")

        assertEquals(3, constructor.parameters.size)

        assertTrue(constructor.hasThisParameter)
        assertEquals("\$this", constructor.parameters[0].id)
        assertEquals(point.type, constructor.parameters[0].type)

        assertEquals("x", constructor.parameters[1].id)
        assertEquals(INT, constructor.parameters[1].type)

        assertEquals("y", constructor.parameters[2].id)
        assertEquals(INT, constructor.parameters[2].type)

        val x = 10
        val y = 20
        vm.execute(constructor, point, vm.getValue(x), vm.getValue(y))

        assertEquals(x, point.target.getField(type["x"]).value)
        assertEquals(y, point.target.getField(type["y"]).value)

        val getX = module.getProcedure("x")
        val getY = module.getProcedure("y")

        assertEquals(x, vm.execute(getX, point)?.value)
        assertEquals(y, vm.execute(getY, point)?.value)
    }

    @Test
    fun testEqualityWithPrimitiveFields() {
        val src = """
            record Point(int x, int y) { }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Point")

        println(module)

        val vm = IVirtualMachine.create()
        val pointA = vm.allocateRecord(type)
        val pointB = vm.allocateRecord(type)

        val constructor = module.getProcedure("\$init")

        vm.execute(constructor, pointA, vm.getValue(10), vm.getValue(20))
        vm.execute(constructor, pointB, vm.getValue(10), vm.getValue(20))

        val equals = module.getProcedure("equals")

        assertTrue(vm.execute(equals, pointA, pointB)?.value as Boolean)  // Strudel equals() succeeds
        assertNotEquals(pointA, pointB) // Kotlin == fails because it compares them by reference

        assertEquals(pointA.target.getField(type["x"]).value, pointB.target.getField(type["x"]).value)
        assertEquals(pointA.target.getField(type["y"]).value, pointB.target.getField(type["y"]).value)
    }

    @Test
    fun testEqualityWithRecordFields() {
        val src = """
            record Point(int x, int y) { }
            record Line(Point start, Point end, int id) { }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val pointType = module.getRecordType("Point")
        val lineType = module.getRecordType("Line")

        println(module)

        val vm = IVirtualMachine.create()

        val pointA = vm.allocateRecord(pointType)
        val pointB = vm.allocateRecord(pointType)
        val pointConstructor = module.getProcedure("\$init", "Point")
        vm.execute(pointConstructor, pointA, vm.getValue(0), vm.getValue(0))
        vm.execute(pointConstructor, pointB, vm.getValue(5), vm.getValue(0))

        val lineA = vm.allocateRecord(lineType)
        val lineB = vm.allocateRecord(lineType)
        val lineConstructor = module.getProcedure("\$init", "Line")
        vm.execute(lineConstructor, lineA, pointA, pointB, vm.getValue(999))
        vm.execute(lineConstructor, lineB, pointA, pointB, vm.getValue(999))

        val equals = module.getProcedure("equals", "Line")
        assertTrue(vm.execute(equals, lineA, lineB)?.value as Boolean)
        assertNotEquals(lineA, lineB)
    }

    @Test
    fun testCompactConstructor() {
        val src = """
            record Interval(double start, double end) {
                public Interval {
                    if (start > end)
                        throw new IllegalArgumentException("Start must be smaller than or equal to end!");
                } 
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Interval")

        println(module)

        val vm = IVirtualMachine.create()

        val interval = vm.allocateRecord(type)

        val constructor = module.getProcedure("\$init")
        assertThrows<ExceptionError> {
            vm.execute(constructor, interval, vm.getValue(10), vm.getValue(0))
        }
    }

    @Test
    fun testMethods() {
        val src = """
            record Interval(double start, double end) {
                public double length() {
                    return end - start;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val type = module.getRecordType("Interval")

        println(module)

        val vm = IVirtualMachine.create()

        val interval = vm.allocateRecord(type)

        val constructor = module.getProcedure("\$init")
        vm.execute(constructor, interval, vm.getValue(0), vm.getValue(10))

        val length = module.getProcedure("length")
        assertEquals(10.0, vm.execute(length, interval)?.value)
    }

    @Test
    fun testMultipleTypes() {
        val src = """
            record QQ(int x, int y) {}

            class Test {
                static QQ test() {
                    QQ p = new QQ(3,2);
                    return p;
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        val qqType = module.getRecordType("QQ")

        println(module)

        val vm = IVirtualMachine.create()

        val test = module.getProcedure("test", "Test")
        val qq = vm.execute(test)

        assertIs<IReference<IRecord>>(qq)
        assertEquals(3, qq.target.getField(qqType["x"]).value)
        assertEquals(2, qq.target.getField(qqType["y"]).value)

        println(module)
    }
}
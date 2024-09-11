package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestImplicitCasts {
    val code = """ 
         class Test {
            static double d() {
                return 2;
            }
            
            static int c() {
                return 'a';
            }
            
            static double f() {
                return 'a';
            }
         }
    """.trimIndent()

    @Test
    fun testDoubleToInt() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)

        val r = vm.execute(module["d"])
        assertEquals(2.0, r?.toDouble())
        assertEquals(DOUBLE, r?.type)
    }

    @Test
    fun testCharToInt() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)

        val r = vm.execute(module["c"])
        assertEquals(97, r?.toInt())
        assertEquals(INT, r?.type)
    }

    @Test
    fun testCharToDouble() {
        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)

        val r = vm.execute(module["f"])
        assertEquals(97.0, r?.toDouble())
        assertEquals(DOUBLE, r?.type)
    }
}
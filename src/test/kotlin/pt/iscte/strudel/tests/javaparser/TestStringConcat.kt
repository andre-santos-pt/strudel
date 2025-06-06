package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.tests.referenceValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.sqrt
import org.junit.jupiter.api.Assertions.assertEquals

class TestStringConcat {

    @Test
    fun testStringString() {
        val src = """
            class StringConcat {
                public static String concatStringString(String x, String y) {
                    return x + y;
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()
        val concat = module.getProcedure("concatStringString")

        val result = vm.execute(concat, getString("hello "), getString("world"))
        assertEquals("hello world", result?.referenceValue)
    }

    @Test
    fun testStringPrimitive() {
        val src = """
            class StringConcat {
                public static String concatStringPrimitive(String x, int y) {
                    return x + y;
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()
        val concat = module.getProcedure("concatStringPrimitive")

        val result = vm.execute(concat, getString("hello "), vm.getValue(2))
        assertEquals("hello 2", result?.referenceValue)
    }

    @Test
    fun testStringAny() {
        val src = """
            import java.lang.Math; 
            
            class StringConcat {
                public static String concatLeft(String x) {
                    return x + Math.sqrt(2.0);
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()
        val concat = module.getProcedure("concatLeft")

        val result = vm.execute(concat, getString("hello "))
        assertEquals("hello ${sqrt(2.0)}", result?.referenceValue)
    }

    @Test
    fun testAnyString() {
        val src = """
            import java.lang.Math; 
            
            class StringConcat {
                public static String concatRight(String x) {
                    return Math.sqrt(2.0) + x;
                }
            }
        """.trimIndent()

        val module = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()
        val concat = module.getProcedure("concatRight")

        val result = vm.execute(concat, getString(" hello"))
        assertEquals("${sqrt(2.0)} hello", result?.referenceValue)
    }
}
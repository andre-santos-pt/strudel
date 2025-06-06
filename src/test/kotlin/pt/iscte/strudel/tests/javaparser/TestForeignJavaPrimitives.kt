package pt.iscte.strudel.tests.javaparser

import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.allocateString
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TestForeignJavaPrimitives {

    val src = """
        import java.util.Arrays;
        
        public class Test {
            static int stringLength() {
                return "123".length();
            }
            
            static boolean stringEmpty() {
                return "123".isEmpty();
            }
            
            static char stringChar() {
                return "abc".charAt(0);
            }
            
            static String subString() {
                return "abcd".substring(2);
            }
            
            static String subStringLeft() {
                return "abcd".substring(0, 2);
            }
            
            static int countWords() {
                String s = "um dois tres";
                String[] parts = s.split(" ");
                return parts.length;
            }
            
             static String[] words() {
                String s = "um dois tres";
                String[] parts = s.split(" ");
                return parts;
            }
            
            static double rand() {
                return Math.random() * 0.0;
            }
            
            static int[] intArrayCopy() {
                return Arrays.copyOf(new int[] {1,2,3}, 4);
            }
            
            static double[] arraySort() {
                double[] a = {3.2,4.1,2.4,1.1};
                Arrays.sort(a);
                return a;
            }
        }
    """.trimIndent()
    @Test
    fun test() {
        val m = Java2Strudel().load(src, "Test")
        println(m)
        val vm = IVirtualMachine.create(availableMemory = 4000)
        val expected = listOf(
            vm.getValue(3),
            vm.getValue(false),
            vm.getValue('a'),
            vm.allocateString("cd"),
            vm.allocateString("ab"),
            vm.getValue(3),
            vm.allocateStringArray("um", "dois", "tres"),
            vm.getValue(0.0),
            vm.allocateArrayOf(INT, 1,2,3,0),
            vm.allocateArrayOf(DOUBLE, 1.1,2.4,3.2,4.1)
        )
        m.procedures.filter { it.id != "\$init"  && !it.isForeign}.forEachIndexed { i, p ->
            val ret = vm.execute(p)
            if(ret is IReference<*>)
                assertEquals((expected[i] as IReference<*>).target, ret.target)
            else
                assertEquals(expected[i], ret)
            println("${ret?.type} $ret")
        }

//        m.procedures.last{ !it.isForeign}.let { p->
//            val ret = vm.execute(p)
//            if(ret is IReference<*>)
//                assertEquals((expected.last() as IReference<*>).target, ret.target)
//            else
//                assertEquals(expected.last(), ret)
//            println("${ret?.type} $ret")
//        }
    }

}
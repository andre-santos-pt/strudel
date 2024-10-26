package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.allocateString
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.vm.IVirtualMachine

class TestMultiNameResolves {


    @Test
    fun test() {
        val code = """    
       class joine {
            static String join(String[] elements, String separator) {
                if(elements.length == 0)
                    return "";
                int l = separator.length();
                String join = joine.same(elements[0]);
                int m = Math.min(l, 10);
             for(int i = 1; i < elements.length; i++)
                join = Other.same(join) + same(separator) + elements[i]; 
                return join;
        }

        static String same(String s) {
            return s;
        }
    }
    
    class Other {
        static String same(String s) {
            return s;
        }
    }
    """.trimIndent()

        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)
        val args = arrayOf(
            vm.allocateStringArray("java", "python", "kotlin"),
            vm.allocateString(", ")
        )
        val r = vm.execute(module.getProcedure("join"), *args)
        println(r)

    }

    @Test
    fun testJoin() {
        val code = """    
       class join {static
String join(String[] elements, String separator) {
    if(elements.length == 0)
        return "";
   
    String join = elements[0];
    for(int i = 1; i < elements.length; i++)
        join = join + separator + elements[i]; 
    return join;
}


}
    """.trimIndent()

        val vm = IVirtualMachine.create()
        val module = Java2Strudel().load(code)
        println(module)
        val args = arrayOf(
            vm.allocateStringArray("java", "python", "kotlin"),
            vm.allocateString(", ")
        )
        val r = vm.execute(module.getProcedure("join"), *args)
        println(r)

    }
}
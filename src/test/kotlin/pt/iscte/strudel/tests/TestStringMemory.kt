package pt.iscte.strudel.tests

import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.allocateString
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.impl.VirtualMachine
import org.junit.jupiter.api.Test

class TestStringMemory {

    val src = """
        public class Test {
        static int countWords(String text) {
            if(text.isEmpty())
                return 0;
            int count = 0;
            for(int i = 1; i < text.length(); i++)
                if(text.charAt(i) == ' ')
                    count++;
            
            
            if(text.charAt(text.length() - 1) != ' ')
                count++;
            return count;
        }
        }
    """.trimIndent()
    @Test
    fun test() {
        val m = Java2Strudel().load(src, "Test")
        println(m)
        val vm = IVirtualMachine.create()
        vm.execute(m["countWords"],vm.allocateString("a introdução à programação "))
    }

}
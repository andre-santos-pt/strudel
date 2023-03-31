package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.NULL
import pt.iscte.strudel.vm.impl.VirtualMachine

fun main() {
    val javaCode = """
        public class HelloWorld {   
           public static void main(String[] args) {
                System.out.println("Hello World".concat("!"));
           }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    val vm = VirtualMachine(throwExceptions = true)
    vm.execute(module.getProcedure("main"), NULL)

}

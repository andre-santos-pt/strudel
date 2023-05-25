package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL

fun main() {
    val javaCode = """
        public class HelloWorld {   
           public static void main(String[] args) {
                System.out.println("Hello World".concat("!"));
           }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    val vm = IVirtualMachine.create()
    vm.execute(module.getProcedure("main"), NULL)

}

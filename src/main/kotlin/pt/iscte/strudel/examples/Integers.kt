package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.NULL
import pt.iscte.strudel.vm.IVirtualMachine

fun main() {
    val javaCode = """
        public class Integers {   
           public static void main(String[] args) {
                int[] array = {1,2,3};
                  
                for(int j : array) {
                    println(j);
                    for(int k : array)
                        println(k);
                }
                
                for(int i = 0; i != array.length; i++) {
                    print(i);
                    print(": ");
                    println(array[i]);
                }
           }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    println(module)
    val vm = IVirtualMachine.create()
    vm.execute(module.getProcedure("main"), NULL)

}

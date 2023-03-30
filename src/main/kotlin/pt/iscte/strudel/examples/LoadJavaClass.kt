package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.ANY
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.VirtualMachine

fun main() {
    val javaCode = """
        class IntArrayList {
            int[] elements;
            int next;
            
            IntArrayList() {
                elements = new int[10];
                next = 0;
            }
            
             IntArrayList(int cap) {
                elements = new int[cap];
                next = 0;
            }
            
            
            int size() {
                return next;
            }
            
            boolean isFull() {
                return next == elements.length;
            }
            
            int get(int index) {
                return elements[index];
            }
            
            void add(int e) {
                this.elements[next] = e;
                next = next + 1;
            }
            
            void doubleCapacity() {
                int[] newElements = new int[elements.length*2];
                for(int i = 0; i < next; i++)
                    newElements[i] = elements[i];
                elements = newElements;
            }
            
            
        }
        
        class Test {
            public static void main() {
                IntArrayList list = new IntArrayList();
                list.add(1);
                list.add(2);
                list.add(3);
                list.doubleCapacity();
                for(int i = 0; i < list.size(); i++)
                    System.out.println(list.get(i));
            }
        }
        
    """.trimIndent()


    val module = Java2Strudel(foreignProcedures = listOf(
        ForeignProcedure("System.out", "println", VOID, ANY) {
                m, args -> println(args[0])
        }
    )).load(javaCode)

    println(module)
    val vm = VirtualMachine()
    val main = module.getProcedure("main", "Test")
    vm.execute(main)

}
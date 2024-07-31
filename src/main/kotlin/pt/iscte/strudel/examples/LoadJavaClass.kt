package pt.iscte.strudel.examples

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.*

fun main() {
    val javaCode = """
        class IntArrayList {
            int[] elements;
            int next;
            
            IntArrayList() {
                elements = new int[5];
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
                if(isFull())
                    this.doubleCapacity();
                    
                this.elements[next] = e;
                next++;
            }
            
            void doubleCapacity() {
                int[] newElements = new int[elements.length*2];
                for(int i = 0; i < next; ++i)
                    newElements[i] = elements[i];
                elements = newElements;
            }
            
            
        }
        
        class Test {
            public static void main() {
                IntArrayList list = new IntArrayList();
                 for(int i = 1; i <= 10; i++) {
                 
                    list.add(i);
                    }
                    
                for(int j = 0; j < list.size(); j++)
                    System.out.println(list.get(j));
            }
        }
        
    """.trimIndent()


    val module = Java2Strudel().load(javaCode)

    println(module)
    val vm = IVirtualMachine.create()
    vm.addListener(object : IVirtualMachine.IListener {
        override fun arrayAllocated(ref: IReference<IArray>) {
            println(ref.target)
            ref.target.addListener(object : IArray.IListener {
                override fun elementChanged(
                    index: Int,
                    oldValue: IValue,
                    newValue: IValue
                ) {
                    println(ref.target)
                }
            })
        }
    })
    val main = module.getProcedure("main", "Test")
    try {
        vm.execute(main)
    } catch (e: RuntimeError) {
        println(e)
        vm.callStack.frames.reversed().forEach {
            println(it.procedure.id + "(" + it.arguments.joinToString { it.toString() } + ")")
        }
    }
}
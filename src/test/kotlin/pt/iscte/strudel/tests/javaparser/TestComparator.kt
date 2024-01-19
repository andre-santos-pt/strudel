package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import kotlin.test.*

class TestComparator {
    val code = """    
        class Larger implements Comparator<String> {
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        }
        
        class Smaller implements Comparator<String> {
            public int compare(String a, String b) {
                return b.length() - a.length();
            }
        }
        
        class Test {
            static void bubbleSort(String[] arr, Comparator<String> c) {
                int n = arr.length;
                int temp = 0;
                for(int i=0; i < n; i++){
                    for(int j=1; j < (n-i); j++){
                        if(c.compare(arr[j-1], arr[j]) > 0){  
                            temp = arr[j-1];  
                            arr[j-1] = arr[j];  
                            arr[j] = temp;  
                        }
                    }      
                }
            }
            
            static String[] test1() {
                String[] array = {"dois", "quatro", "um", "tres" };
                System.out.println(array[0]);
                bubbleSort(array, new Smaller());
                return array;
            }
            
             static String[] test2() {
                String[] array = {"dois", "quatro", "um", "tres" };
                System.out.println(array[0]);
                bubbleSort(array, new Larger());
                return array;
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()

        val ret = vm.execute(model.procedures.find { it.id == "test1" }!!)
        println(ret)

//        assertEquals(vm.getValue(2).toInt(), ret1?.toInt())
//
//        val ret2 = vm.execute(model.procedures.find { it.id == "test2" }!!)
//        assertEquals(vm.getValue(0).toInt(), ret2?.toInt())
    }
}
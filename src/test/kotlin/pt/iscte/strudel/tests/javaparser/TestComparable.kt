package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

class TestComparable {
    val code = """    
        import java.util.Comparable;
  
        class Test {
            static void bubbleSort(Comparable[] arr) {
                int n = arr.length;
                int temp = 0;
                for(int i=0; i < n; i++){
                    for(int j=1; j < (n-i); j++){
                        if(arr[j-1].compareTo(arr[j]) > 0){  
                            temp = arr[j-1];  
                            arr[j-1] = arr[j];  
                            arr[j] = temp;  
                        }
                    }      
                }
            }
            
            static String[] test1() {
                String[] array = {"dois", "quatro", "um", "tres" };
                bubbleSort(array);
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
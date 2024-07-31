package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine

class TestComparable {
    val code = """    
        import java.util.Comparable;
  
        class Test {
            static void bubbleSort(String[] arr) {
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

        val ret = (vm.execute(model.procedures.find { it.id == "test1" }!!  as IProcedure) as IReference<IArray>).target
        listOf("dois","quatro","tres","um").forEachIndexed { i, n ->
            Assertions.assertEquals(n, (ret.getElement(i).value.toString()))
        }
    }
}
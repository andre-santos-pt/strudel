package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.tests.checkStringArrayContent
import pt.iscte.strudel.vm.IVirtualMachine

class TestComparator {
    val code = """    
        import java.util.Comparator;
        
        class Smaller implements Comparator<String> {
            public int compare(String a, String b) {
                return a.length() - b.length();
            }
        }
        
        class Larger implements Comparator<String> {
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
                String[] array = {"cinco", "quatro", "um", "tres" };
                bubbleSort(array, new Smaller());
                return array;
            }
            
             static String[] test2() {
                String[] array = {"cinco", "quatro", "um", "tres" };
                bubbleSort(array, new Larger());
                return array;
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()

        val ret1 = vm.execute(model.procedures.find { it.id == "test1" }!! as IProcedure)
        ret1?.checkStringArrayContent("um", "tres", "cinco", "quatro")

        val ret2 = vm.execute(model.procedures.find { it.id == "test2" }!! as IProcedure)
        ret2?.checkStringArrayContent("quatro", "cinco", "tres", "um")
    }
}
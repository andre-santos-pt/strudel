package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.StringType
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestComparablePolymorphicParameter {
    val code = """    
        import java.util.Comparable;
  
        class Test {
            static void bubbleSort(Comparable[] arr) {
                int n = arr.length;
                Comparable temp = 0;
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

    val insertion = """
        import java.util.Comparable;
        
        class InsertionUpdated {
            public static void sort(Comparable[] arr) {
                for (int i = 0; i < arr.length; i++) {
                    Comparable aux = arr[i];
                    int j;
                    for (j = i; j > 0 && aux.compareTo(arr[j - 1]) < 0; j--) {
                        arr[j] = arr[j - 1];
                    }
                    arr[j] = aux;
                }
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

    @Test
    fun testPolymorphicForeignUsage() {
        val module = Java2Strudel().load(insertion)
        val vm = IVirtualMachine.create()

        val x = vm.allocateArrayOf(INT, 7, 3, 2, 1, 5, 6, 10, 8, 9, 4)

        val sort = module.getProcedure(("sort"))
        vm.execute(sort, x)

        (1..10).forEach {
            assertEquals(it, x.target.getElement(it - 1).value)
        }
    }

    @Test
    fun testPolymorphicForeignUsage2() {
        val module = Java2Strudel().load(insertion)
        val vm = IVirtualMachine.create()

        val x = vm.allocateArrayOf(StringType, getString("dois"), getString("quatro"), getString("um"), getString("tres"))

        val sort = module.getProcedure(("sort"))
        vm.execute(sort, x)

        println(x)

        assertEquals("dois", x.target.getElement(0).value)
        assertEquals("quatro", x.target.getElement(1).value)
        assertEquals("tres", x.target.getElement(2).value)
        assertEquals("um", x.target.getElement(3).value)
    }
}
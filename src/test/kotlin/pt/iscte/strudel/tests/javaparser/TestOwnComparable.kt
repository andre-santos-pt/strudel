package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine

class TestOwnComparable {
    val code = """    
        import java.util.Comparable;
  
        class Numero implements Comparable<Numero> {
            int valor;
            public Numero(int valor) {
                this.valor = valor;
            }
            int compareTo(Numero n) {
                return valor - n.valor;
            } 
        }
        
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
            
            static Numero[] test1() {
                Numero[] array = {new Numero(4), new Numero(1), new Numero(8), new Numero(5) };
                bubbleSort(array);
                return array;
            }
            
             static void test2() {
                Comparable a = new Numero(4);
                Comparable b = new Numero(5);
                a.compareTo(b);
            }
        }
    """.trimIndent()

    @Test
    fun testOnExpression() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val type = model.getRecordType("Numero")
        val ret = (vm.execute(model.procedures.find { it.id == "test1" }!! as IProcedure) as IReference<IArray>).target

        listOf(1,4,5,8).forEachIndexed { i, n ->
            assertEquals(n, (ret.getElement(i).value as IRecord).getField(type["valor"]).toInt())
        }
    }

    @Test
    fun testOnStatement() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        vm.execute(model.procedures.find { it.id == "test2" }!! as IProcedure)

    }
}
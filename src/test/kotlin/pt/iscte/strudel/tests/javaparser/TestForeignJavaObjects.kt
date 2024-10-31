package pt.iscte.strudel.tests.javaparser

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.allocateString
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class TestForeignJavaObjects {

    val src = """
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.Scanner;
        
        public class Test {
            
             static String testAccess() {
                ArrayList<String> list = new ArrayList<>();
                list.add("um");
                list.add("dois");
                list.add("tres");
                return list.get(1);
            }
            
            static int testLen() {
                ArrayList<String> list = new ArrayList<>();
                list.add("um");
                list.add("dois");
                list.add("tres");
                return list.size();
            }
            
             static String[] testSort() {
                ArrayList<String> list = new ArrayList<>();
                list.add("um");
                list.add("dois");
                list.add("tres");
                Collections.sort(list);
                return list.toArray(new String[0]);
            }
           
            static String[] testCall() {
                ArrayList<String> list = new ArrayList<>();
                list.add("um");
                list.add("dois");
                list.add("tres");
                _sort(list);
                return list.toArray(new String[0]);
            }
            
            static void _sort(ArrayList<String> list) {
                Collections.sort(list);
            }
            
            static int testCreate() {
                 ArrayList<String> list = new ArrayList<>();
                 String s = "test";
                 for(int i = 0; i < 100; i++)
                    list.add(s);
                 return list.toArray(new String[0]).length;
            }
            
            static int testScanner() {
                String s = "um dois tres";
                int c = 0;
                Scanner scan = new Scanner(s);
                while(scan.hasNext()) {
                    scan.next();
                    c++;
                }
                scan.close();
                return c;
            }
                 
        }
    """.trimIndent()
    @Test
    fun test() {
        val m = Java2Strudel().load(src, "Test")
        println(m)
        val vm = IVirtualMachine.create(availableMemory = 10000)
        val expected = listOf(
            vm.allocateString("dois"),
            vm.getValue(3),
            vm.allocateStringArray("dois", "tres", "um"),
            vm.allocateStringArray("dois", "tres", "um"),
            vm.getValue(100),
            vm.getValue(3)
        )
        m.procedures.filter { it.id != "\$init"  && it.id?.startsWith("_") == false && !it.isForeign}.forEachIndexed { i, p ->
            val ret = vm.execute(p)
            if(ret is IReference<*>)
                assertEquals((expected[i] as IReference<*>).target, ret.target)
            else
                assertEquals(expected[i], ret)
            println("${ret?.type} $ret")
        }

//        m.procedures.last{ !it.isForeign}.let { p->
//            val ret = vm.execute(p)
//            if(ret is IReference<*>)
//                assertEquals((expected.last() as IReference<*>).target, ret.target)
//            else
//                assertEquals(expected.last(), ret)
//            println("${ret?.type} $ret")
//        }
    }

}
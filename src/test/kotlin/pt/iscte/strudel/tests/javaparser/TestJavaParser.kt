package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.vm.impl.VirtualMachine
import java.io.File


class TestJavaParser {

    val code = """
public class MyArrayList {
    private Object[] elements = new Object[10];
    private int next = 0;
    
    void add (Object e) {
       elements[next] = e;
       next++;
    }
}

"""
    @Test
    fun testString() {
        val m = Java2Strudel().load(code)
        println(m)
//        println(((m.procedures[0].block.children[0].getProperty("SourceLocation"))))
//        println(((m.procedures[0].block.children[0] as IVariableAssignment).expression.getProperty("SourceLocation")))
//        println(((m.procedures[0].block.children[0] as IVariableAssignment).expression as IBinaryExpression).getProperty("OPERATOR_LOC"))
    }


    @Test
    fun testFile() {
        val m = Java2Strudel().load(File("src/test/java/BinarySearch.java"))
        println(m)
        val vm = VirtualMachine()
        val array = vm.allocateArrayOf(INT, 1, 3, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22)
        val r =
            vm.execute(m.procedures[0], array, vm.getValue(10))
        println(r)
    //        println(((m.procedures[0].block.children[0].getProperty("SourceLocation"))))
//        println(((m.procedures[0].block.children[0] as IVariableAssignment).expression.getProperty("SourceLocation")))
//        println(((m.procedures[0].block.children[0] as IVariableAssignment).expression as IBinaryExpression).getProperty("OPERATOR_LOC"))
    }

}
package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.INIT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import org.junit.jupiter.api.Assertions.assertTrue

class TestExplicitConstructor {

    @Test
    fun test() {
        val src = """
            record MatrixSize(int rows, int columns) { }
            
            class Matrix {
                private MatrixSize size;
                private int[] values;
                
                Matrix(int rows, int columns) {
                    this(new MatrixSize(rows, columns));
                }
                
                Matrix(MatrixSize size) {
                    this.size = size;
                    this.values = new int[10];
                }
            }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)

        val matrixSizeType = module.getRecordType("MatrixSize")
        val matrixType = module.getRecordType("Matrix")

        val matrixSizeConstructor = module.getProcedure(INIT, "MatrixSize")
        val matrixConstructor1 = module.getProcedure(INIT, "Matrix") { it.parameters.size == 3 }
        val matrixConstructor2 = module.getProcedure(INIT, "Matrix") { it.parameters.size == 2 }

        val vm = IVirtualMachine.create()

        val matrixSize = vm.allocateRecord(matrixSizeType)
        vm.execute(matrixSizeConstructor, matrixSize, vm.getValue(3), vm.getValue(3))

        println(matrixSize)

        val matrix1 = vm.allocateRecord(matrixType)
        val matrix2 = vm.allocateRecord(matrixType)

        vm.execute(matrixConstructor1, matrix1, vm.getValue(3), vm.getValue(3))
        vm.execute(matrixConstructor2, matrix2, matrixSize)

        val matrixSize1 = matrix1.target.getField(matrixType["size"])
        val matrixSize2 = matrix2.target.getField(matrixType["size"])

        val equals = module.getProcedure("equals", "MatrixSize")
        assertTrue(vm.execute(equals, matrixSize1, matrixSize2)?.value as Boolean)
    }
}
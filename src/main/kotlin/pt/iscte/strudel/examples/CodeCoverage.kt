package pt.iscte.strudel.examples

import com.github.javaparser.ast.Node
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IStatement
import pt.iscte.strudel.vm.IVirtualMachine

/**
 * Returns a map with the number of times each statement was executed.
 */
fun codeCoverage(module: IModule, mainProcedure: String): Map<IStatement, Int> {
    val vm = IVirtualMachine.create()
    val statementCount = mutableMapOf<IStatement, Int>()
    vm.addListener(object : IVirtualMachine.IListener {
        override fun statement(s: IStatement) {
            statementCount.putIfAbsent(s, 0)
            statementCount[s] = statementCount[s]!! + 1
        }
    })
    vm.execute(module[mainProcedure])
    return statementCount
}

class CodeCoverageExample {
    companion object {
        val example = """
    class Test {
         static void main() {
            int i = 1;
            int even = 0;
            int odd = 0;
            while(i <= 10) {
                if(isEven(i))
                    even++;
                else
                    odd++;
                i++;
            }
        }
        
        static boolean isEven(int n) {
            if(n % 2 == 0)
                return true;
            else
                return false;
        }
     
        
    }
    """.trimIndent()
    }
}

fun main() {
    val module = Java2Strudel().load(CodeCoverageExample.example)
    val statementCount = codeCoverage(module, "main")
    statementCount.forEach { s, c ->
        // JavaParser node to get original source code line
        val node = s.getProperty("JP") as Node
        println("${node.range.get().begin.line}: $node - $c times")
    }
}
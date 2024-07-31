package pt.iscte.strudel.examples

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

fun main() {
    val javaCode = """
        public class Factorial {   
           static int fact(int n) {
                int i = n;
                int f = 1;
                while(i > 1) {
                    f *= i;
                    i--;
                }
                return f;
           }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    val vm = IVirtualMachine.create()
    vm.addListener(object : IVirtualMachine.IListener {
        var vars: List<IVariableDeclaration<IBlock>>? = null

        override fun procedureCall(
            procedure: IProcedureDeclaration,
            args: List<IValue>,
            caller: IProcedure?
        ) {
            if(procedure is IProcedure) {
                vars = procedure.localVariables
                println(procedure.id + "(${args.joinToString()})")
                println(vars?.joinToString(separator = " | ") { "${it.id}" })
            }
        }

        override fun loopIteration(loop: ILoop) {
            println(vars?.joinToString(separator = " | "){ "${vm.topFrame[it]}" })
        }

        override fun procedureEnd(
            procedure: IProcedureDeclaration,
            args: List<IValue>,
            result: IValue?
        ) {
            println(vars?.joinToString(separator = " | "){ "${vm.topFrame[it]}" })
            println("result: $result")
        }
    })
    vm.execute(module.getProcedure("fact"), vm.getValue(5))
    vm.execute(module.getProcedure("fact"), vm.getValue(3))

}

package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.util.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestExpressionMaterialize {

    @Test
    fun test() {
        val code = """
        class Test {
            static int naturalsSum(int max) {
                int[] nats = new int[max];
                for(int i = 0; i < max; i++) {
                   nats[i] = i+1;
                }
                return sum(nats);
            }
            
            static int sum(int[] nats) {
                int sum = 0;
                for(int i = 0; i < nats.length; i++) {
                    sum = sum + nats[i];
                }
                return sum;
            }
            
        }
    """.trimIndent()
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val varExp = mutableMapOf<IExpressionHolder, MutableList<IExpression>>()

        vm.addListener(object : IVirtualMachine.IListener {
            override fun expressionEvaluation(
                e: IExpression,
                context: IExpressionHolder,
                value: IValue,
                concreteExpression: IExpression
            ) {
                varExp.putIfAbsent(context, mutableListOf())
                varExp[context]!!.add(concreteExpression)
            }
        })

        val naturalsSum = model.procedures.find { it.id == "naturalsSum" }!! as IProcedure
        vm.execute(naturalsSum, vm.getValue(5))

        val natsAss = naturalsSum.find(IArrayElementAssignment::class)
        assertEquals(listOf("0 + 1", "1 + 1", "2 + 1", "3 + 1", "4 + 1"), varExp[natsAss]!!.map { it.toText() })

        val forLoop = naturalsSum.find(ILoop::class)
        assertEquals(listOf("0 < 5", "1 < 5", "2 < 5", "3 < 5", "4 < 5", "5 < 5"), varExp[forLoop]!!.map { it.toText() })

        val sum = model.procedures.find { it.id == "sum" }!! as IProcedure
        val sumAss = sum.find(IVariableAssignment::class, 2)
        assertEquals(listOf("0 + nats[0]", "1 + nats[1]", "3 + nats[2]", "6 + nats[3]", "10 + nats[4]"), varExp[sumAss]!!.map { it.toText() })
    }



    @Test
    fun testShift() {
        val code = """
        class Test {
            static void shift(int[] numbers) {
                int first = numbers[0];
                for(int i = 0; i < numbers.length-1; i++) {
                    numbers[i] = numbers[i+1];
                }
                numbers[numbers.length-1] = first;
            }
            
        }
    """.trimIndent()
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val varExp = mutableMapOf<IExpressionHolder, MutableList<IExpression>>()

        vm.addListener(object : IVirtualMachine.IListener {
            override fun expressionEvaluation(
                e: IExpression,
                context: IExpressionHolder,
                value: IValue,
                concreteExpression: IExpression
            ) {
                varExp.putIfAbsent(context, mutableListOf())
                varExp[context]!!.add(concreteExpression.solveIntArithmeticConstants())
            }
        })
        val shift = model.procedures.find { it.id == "shift" }!! as IProcedure
        vm.execute(shift,  vm.allocateArrayOf(INT, 1,2,3,4,5))

        val arrayAss0 = shift.find(IArrayElementAssignment::class, 0)
        val arrayAss1 = shift.find(IArrayElementAssignment::class, 1)
        assertEquals(listOf("numbers[1]", "numbers[2]", "numbers[3]", "numbers[4]", "1"),
            varExp[arrayAss0]!!.map { it.toText() } + varExp[arrayAss1]!!.map { it.toText() })
    }
}
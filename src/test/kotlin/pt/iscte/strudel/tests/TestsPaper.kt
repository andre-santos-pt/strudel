package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.vm.*
import pt.iscte.strudel.vm.impl.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestsPaper {
    val binarySearchCode = """
        class Example {
        static boolean binarySearch(int[] a, int e) { 
          int l = 0;
          int r = a.length - 1; 
          while(l <= r) { 
            int m = l + ((r-l) / 2); 
            if(a[m] == e) return true; 
            if(a[m] < e) l = m + 1;
            else r = m - 1;
          } 
          return false;
        }
        }
        """
    fun checkMissingReturns(procedure: IProcedure): Boolean {
        require(procedure.returnType != VOID)
        val cfg = procedure.createCFG()
        return cfg.exitNode.incoming.all { it.element is IReturn }
    }


    fun modifiesParameters(procedure: IProcedure): Boolean {
        var modifies = false
        procedure.accept(object : IBlock.IVisitor {
            override fun visit(a: IVariableAssignment): Boolean {
                if (a.target in procedure.parameters)
                    modifies = true
                return false
            }
        })
        return modifies
    }

    val bsearch = Procedure(BOOLEAN) {
        val a = Param(array(INT))
        val e = Param(INT)
        val l = Var(INT, 0)
        val r = Var(INT, a.length() - 1)
        While(l smallerEq r) {
            val m = Var(INT, l + (r - l) / 2)
            If(a[m] equal e) {
                Return(True)
            }
            If(a[m] smaller e) {
                Assign(l, m + 1)
            }.Else {
                Assign(r, m - 1)
            }
        }
        Return(False)
    }






    @Test
    fun testRun() {
        val binarySearch = Java2Strudel().load(binarySearchCode).getProcedure("binarySearch")
        println(binarySearch)
        val vm = VirtualMachine(
            callStackMaximum = 1024,
            loopIterationMaximum = 100000,
            availableMemory = 2048
        )
        val array = vm.allocateArrayOf(INT,
            1, 2, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22
        )
        val e: IValue = vm.getValue(20)
        val result: IValue? = vm.execute(binarySearch, array, e)


        assertTrue(result?.toBoolean() == true)
    }

    @Test
    fun testDebug() {
        val binarySearch = Java2Strudel().load(binarySearchCode).getProcedure("binarySearch")
        val vm = VirtualMachine()
        val array = vm.allocateArrayOf(INT,
            1, 2, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22
        )
        val e: IValue = vm.getValue(20)
        val process = ProcedureInterpreter(vm, binarySearch, array, e)
        process.init()
        while (!process.isOver())
            process.step()
        val result: IValue? = process.returnValue

        assertTrue(result?.toBoolean() == true)
    }

    @Test
    fun testListeners() {
        val vm = VirtualMachine()
        var totalArrayLength = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                totalArrayLength += (ref.target as IArray).length
            }
        })
        // vm.execute(...)

    }

    @Test
    fun testArrayError() {
        val code = """
            class Test {
                static void test() {
                   int[] a = new int[10];
                   int[] b = new int[10];
                   b[0] = a[b.length];
                }
            }
        """
        val vm = VirtualMachine(throwExceptions = false)
        val proc = Java2Strudel().load(code).getProcedure("test")
        vm.execute(proc)
        val error = vm.error as ArrayIndexError

        val inst = vm.instructionPointer // b[0] = a[b.length];
        assertEquals("b[0] = a[b.length];", vm.instructionPointer.toString().trim())
        val array = error.target // a
        assertEquals("a", error.target.toString())
        val index = error.invalidIndex // 10
        assertEquals(10, error.invalidIndex)
        val exp = error.indexExpression // b.length
        assertEquals("b.length", exp.toString())
    }

    @Test
    fun testForeignInterface() {
        val code = """
            class Test {
                static void display(int n) {
                    print(n + 1);
                }
            }
        """
        val vm = VirtualMachine()
        val print = ForeignProcedure(null, "print", VOID, ANY) { m, args ->
            println(args[0])
        }
        val loader = Java2Strudel(foreignProcedures = listOf(print))
        val proc = loader.load(code).getProcedure("display")
        val arg = vm.getValue(1)
        vm.execute(proc, arg)
    }

    @Test
    fun testForeignInterfaceList() {
        val code = """
            class Test {
                static void display(int n) {
                 print(n + 1);
                 }
        }
        """
        val vm = VirtualMachine()

        val print = ForeignProcedure(null, "print", VOID, ANY) {
                _, args -> println(args[0])
        }
        val loader = Java2Strudel(
            foreignProcedures = listOf(print)
        )
        val proc = loader.load(code).procedures[0]
        val arg = vm.getValue(1)
        vm.execute(proc, arg)
    }

    @Test
    fun testIterations() {
        val vm = VirtualMachine()
        val counter = vm.addLoopCounter()
        val array = vm.allocateArrayOf(INT, 1, 2, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22)
        val e = vm.getValue(2)
        val result = vm.execute(bsearch, array, e)
        assertTrue(result?.toBoolean() == true)
        assertEquals(3, counter[bsearch.loops[0]])
    }

    val bsearchRec = Procedure(BOOLEAN) {
        val a = Param(array(INT))
        val e = Param(INT)
        val l = Param(INT)
        val r = Param(INT)

        If(l smallerEq r) {
            val m = Var(INT, l + (r - l) / 2)
            If(a[m] equal e) {
                Return(True)
            }.Else {
                If(a[m] smaller e) {
                    Return(this@Procedure.procedure!!.expression(a.expression(),
                        e.expression(),
                        ArithmeticOperator.ADD.on(m.expression(), lit(1)),
                        r.expression()))
                }.Else {
                    Return(this@Procedure.procedure!!.expression(a.expression(),
                        e.expression(), l.expression(),
                        ArithmeticOperator.SUB.on(m.expression(), lit(1))))
                }
            }
        }
        Return(False)
    }

    @Test
    fun testIterationsRec() {
        val vm = VirtualMachine()
        var recCalls = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(
                procedure: IProcedureDeclaration,
                args: List<IValue>,
                caller: IProcedureDeclaration?
            ) {
               if(procedure == bsearchRec && caller == bsearchRec)
                   recCalls++
            }
        })
        val array = vm.allocateArrayOf(INT, 1, 2, 4, 5, 7, 7, 9, 10, 10, 11, 14, 15, 15, 17, 20, 21, 22)
        val e = vm.getValue(2)
        val l = vm.getValue(0)
        val r = vm.getValue(array.target.length-1)
        val result = vm.execute(bsearchRec, array, e, l, r)
        assertTrue(result?.toBoolean() == true)
        assertEquals(2, recCalls)
    }

    @Test
    fun testVariables() {
        val vm = VirtualMachine()
        val tracker = vm.addVariableTracker()
        val a = vm.allocateArrayOf(INT,
            1,2,4,5,7,7,9,10,10,11,14,15,15,17,20,21,22
        )
        val e = vm.getValue(3)
        val result = vm.execute(bsearch, a, e)
        assertTrue(result?.toBoolean() == false)
        assertEquals(listOf(a),
            tracker[bsearch.variables[0]]) // a, immutable
        assertEquals(listOf(3),
            tracker[bsearch.variables[1]].map { it.toInt() }) // e, immutable
        assertEquals(listOf(0, 2),
            tracker[bsearch.variables[2]].map { it.toInt() }) // l
        assertEquals(listOf(16, 7, 2, 1),
            tracker[bsearch.variables[3]].map { it.toInt() }) // r
        assertEquals(listOf(8, 3, 1, 2),
            tracker[bsearch.variables[4]].map { it.toInt() }) // m
    }

    @Test
    fun testAllocations() {
        val code = """
            class Test {
                static void test() {
                    Test[] array = new Test[20];
                    
                    for(int i = 0; i < 10; i++) {
                        array[i] = new Test();
                    }
                }
            }
        """
        val vm = VirtualMachine()
        val module = Java2Strudel().load(code)
        val tracker = vm.addAllocationTracker()
        vm.execute(module.getProcedure("test"))
        assertEquals(10, tracker[module.getType("Test")].size)
        assertEquals(1, tracker[module.getType("Test[]")].size)
    }

    @Test
    fun testJVM() {
        val time = System.currentTimeMillis()
        var i = 0
        var s = 0;
        while (i < 1000) {
            s += i
            i++
        }
        println(System.currentTimeMillis() - time)
        return;
    }


    fun fact(n: Int): Int =
        if(n == 1) 1
        else n*fact(n-1)

    @Test
    fun testJVMfact() {
        val time = System.currentTimeMillis()
        println(fact(1000))
        println(System.currentTimeMillis() - time)
    }

    @Test
    fun testStrudel() {
        val time = System.currentTimeMillis()

        val code = """
           class Test {
           static void test() {
       int i = 0;
        int s = 0;
        while(i < 1000) {
            s += i;
            i++;
        }
        }
        }
        """
        val vm = VirtualMachine()
        val module = Java2Strudel().load(code)
        var i = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                if(a.target.id == "s" && value.toInt() > 100000)
                    println("!!")
            }
        })

        vm.execute(module.getProcedure("test"))
        println(i)
        println(System.currentTimeMillis() - time)
    }
}
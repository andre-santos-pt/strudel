package pt.iscte.strudel.tests.javaparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.tests.checkBooleanArrayContent
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class TestLogicalOperatorsJava {
    val code = """    
        class Test {
            static boolean t() {
                return true;
            }
            
            static boolean f() {
                return false;
            }
        
            static boolean[] testAnd() {
                boolean andFF = f() & f();
                boolean andTF = t() & f();
                boolean andFT = f() & t();
                boolean andTT = t() & t();
                boolean[] ret = {andFF, andTF, andFT, andTT};
                return ret;
            }
            
             // short-circuit
            static boolean testCAnd() {
                boolean andFF = f() && f();
                boolean andTF = t() && f();
                boolean andFT = f() && t();
                boolean andTT = t() && t();
                boolean[] ret = {andFF, andTF, andFT, andTT};
                return ret;
            }
            
            static void testOr() {
                boolean orFF = f() | f();
                boolean orTF = t() | f();
                boolean orFT = f() | t();
                boolean orTT = t() | t();
                boolean[] ret = {orFF, orTF, orFT, orTT};
                return ret;
            }
            
            // short-circuit
            static boolean testCOr() {
                boolean orFF = f() || f();
                boolean orTF = t() || f();
                boolean orFT = f() || t();
                boolean orTT = t() || t();
                boolean[] ret = {orFF, orTF, orFT, orTT};
                return ret;
            }
            
            static void testXor() {
                boolean xorFF = f() ^ f();
                boolean xorTF = t() ^ f();
                boolean xorFT = f() ^ t();
                boolean xorTT = t() ^ t();
                boolean[] ret = {xorFF, xorTF, xorFT, xorTT};
                return ret;
            }
        }
    """.trimIndent()

    class CountCalls : IVirtualMachine.IListener {
        var t = 0
        var f = 0
        override fun procedureCall(procedure: IProcedure, args: List<IValue>, caller: IProcedure?) {
            if (procedure.id == "t") {
                t++
            } else if (procedure.id == "f") {
                f++
            }
        }
    }

    @Test
    fun `test And`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val countCalls = CountCalls()
        vm.addListener(countCalls)

        val ret = vm.execute(model.procedures.find { it.id == "testAnd" }!! as IProcedure)
        ret?.checkBooleanArrayContent(false, false, false, true)
        assertEquals(4, countCalls.t)
        assertEquals(4, countCalls.f)
    }

    @Test
    fun `test CAnd`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val countCalls = CountCalls()
        vm.addListener(countCalls)

        val ret = vm.execute(model.procedures.find { it.id == "testCAnd" }!!  as IProcedure)
        ret?.checkBooleanArrayContent(false, false, false, true)
        assertEquals(3, countCalls.t)
        assertEquals(3, countCalls.f)
    }

    @Test
    fun `test Or`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val countCalls = CountCalls()
        vm.addListener(countCalls)

        val ret = vm.execute(model.procedures.find { it.id == "testOr" }!! as IProcedure)
        ret?.checkBooleanArrayContent(false, true, true, true)
        assertEquals(4, countCalls.t)
        assertEquals(4, countCalls.f)
    }

    @Test
    fun `test COr`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val countCalls = CountCalls()
        vm.addListener(countCalls)

        val ret = vm.execute(model.procedures.find { it.id == "testCOr" }!! as IProcedure)
        ret?.checkBooleanArrayContent(false, true, true, true)
        assertEquals(3, countCalls.t)
        assertEquals(3, countCalls.f)
    }

    @Test
    fun `test Xor`() {
        val model = Java2Strudel().load(code)
        val vm = IVirtualMachine.create()
        val countCalls = CountCalls()
        vm.addListener(countCalls)

        val ret = vm.execute(model.procedures.find { it.id == "testXor" }!!  as IProcedure)
        ret?.checkBooleanArrayContent(false, true, true, false)
        assertEquals(4, countCalls.t)
        assertEquals(4, countCalls.f)
    }
}
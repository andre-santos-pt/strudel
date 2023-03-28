package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.IForeignProcedure
import pt.iscte.strudel.vm.impl.Value
import kotlin.test.assertTrue

val stringType = JavaType(String::class.java)

val StringCreate = IForeignProcedure.create("String", "create", stringType, listOf(CHAR)) { m, args ->
    Value(stringType, args[0].toString())
}

val StringConcat = IForeignProcedure.create("String", "concat", stringType, listOf(stringType, stringType)) { m, args ->
    Value(stringType, (args[0].value.toString()) + (args[1].value.toString()))
}

class TestBuiltinString : pt.iscte.strudel.tests.BaseTest({
    Procedure(StringCreate).setProperty(NAMESPACE_PROP, "String")
    Procedure(StringConcat)
    Procedure(stringType, "strConcat") {
        val str = Var(stringType, "str")
        Assign(str, callExpression(StringCreate, CHAR.literal('a')))
        Assign(str, callExpression(StringConcat, str.expression(), CHAR.literal('b')))
        Return(callExpression(StringConcat, str.expression(), CHAR.literal('c')))
    }
},listOf(StringCreate, StringConcat)) {

    @Test
    fun test() {
        println(module)
        procedure.createCFG().display()
        val r = vm.execute(procedure)
        assertTrue("abc" == r?.value.toString(), r!!.value.toString())
    }
}

class TestBuiltinStringJ {
    @Test
    fun test() {
        val code = """
            class Test {
            static void p() {
                java.lang.String s;
                s = " asdsa";
                }
                }
        """.trimIndent()
        val module = Java2Strudel().load(code)


    }
}

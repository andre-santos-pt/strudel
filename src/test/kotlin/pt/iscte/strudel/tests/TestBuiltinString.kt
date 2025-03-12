package pt.iscte.strudel.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.StringType
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.vm.impl.IForeignProcedure
import pt.iscte.strudel.vm.impl.Value
import kotlin.test.assertTrue

val StringCreate = IForeignProcedure.create("String", "create", StringType, listOf(CHAR)) { m, args ->
    Value(StringType, args[0].value.toString())
}

val StringConcat = IForeignProcedure.create("String", "concat", StringType, listOf(StringType, StringType)) { m, args ->
    Value(StringType, (args[0].value.toString()) + (args[1].value.toString()))
}

class TestBuiltinString : BaseTest({
    Procedure(StringCreate).setProperty(NAMESPACE_PROP, "String")
    Procedure(StringConcat)
    Procedure(StringType, "strConcat") {
        val str = Var(StringType, "str")
        Assign(str, callExpression(StringCreate, CHAR.literal('a')))
        Assign(str, callExpression(StringConcat, str.expression(), CHAR.literal('b')))
        Return(callExpression(StringConcat, str.expression(), CHAR.literal('c')))
    }
},listOf(StringCreate, StringConcat), javaCompile = false) {

    @Test
    fun test() {
        println(module)
        procedure.createCFG().display()
        val r = vm.execute(procedure)
        assertEquals("abc", r?.value.toString())
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
        Java2Strudel().load(code)


    }

    @Test
    fun testConcatPlus() {
        val code = """
            class Test {
            static String p() {
                int i = 3;
                return  "ola" + 12 + "!" + i;
                
                }
                }
        """.trimIndent()
        val module = Java2Strudel().load(code)
        println(module)
        val exp = (module.procedure("p").block.children[2] as IReturn).expression
        assertTrue(exp is IProcedureCall && exp.procedure.id == "concat")
    }
}

package pt.iscte.strudel.tests

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.model.dsl.module
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class BaseTest {
    val module : IModule
    val vm: IVirtualMachine

    constructor(javaSource: String, javaCompile: Boolean = true) {
        module = Java2Strudel(checkJavaCompilation = javaCompile).load(javaSource)
        vm = IVirtualMachine.create()
    }

    constructor(conf: IModule.() -> Unit = {}, foreignProcedures: List<IProcedureDeclaration> = emptyList(), callStackMaximum: Int = 1024, loopIterationMax: Int = 1000000, javaCompile: Boolean = true) {
//        module = module { id = this::class.simpleName }
        val tmp = module {
            id = this::class.simpleName
        }
        conf(tmp)
        tmp.procedures.forEach {
            it.setProperty(NAMESPACE_PROP, tmp.id)
        }
        println(tmp)
        module = Java2Strudel(foreignProcedures = foreignProcedures, checkJavaCompilation = javaCompile).load(tmp.toString())
        vm = IVirtualMachine.create(callStackMaximum = callStackMaximum, loopIterationMaximum = loopIterationMax, throwExceptions = false)
        //conf(module)
        //module.members.addAll(imports)
//        println(Strudel2Java().translate(module))
    }

    val procedure get() = module.procedures.last() as IProcedure

    @BeforeEach
    fun setup() {
        check(module.procedures.isNotEmpty())
        val gencfg = procedure.createCFG()
        assertTrue(gencfg.isValid(), "Invalid CFG:\n$gencfg")
        val cfg = IControlFlowGraph.createEmpty(procedure)
        fillCFG(procedure, cfg)
        if(!cfg.isEmpty) {
            assertTrue(cfg.isValid(), "Invalid mock CFG:\n$cfg")
            assertTrue(gencfg.isValid(), "Invalid CFG:\n$gencfg")
            assertTrue(cfg.isEquivalentTo(gencfg), "CFG does not match")
        }
    }

    @AfterEach
    fun checkErrors() {
        if(vm.error != null)
            println("Execution error: ${vm.error}")
    }

    open fun fillCFG(procedure: IProcedure, cfg: IControlFlowGraph)  { }

    fun call(vararg arguments: String) : IValue? = vm.execute(
        procedure,
        *arguments.map { vm.getValue(it) }.toTypedArray()
    )


    inner class TrackIntVar(val v : IVariableDeclaration<*>, vararg val vals: Int) : IVirtualMachine.IListener {

        constructor(varId: String, vararg vals: Int) : this(procedure.variables.find { it.id == varId }!!, *vals)

        var i = 0

        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (v.ownerProcedure == procedure && a.target == v) {
                val v = vm.topFrame.variables[a.target]!!.toInt()
                assertTrue(i < vals.size, "expected ${a.target.id} not enough")
                assertEquals(v, vals[i], "expected ${a.target.id}=${vals[i++]}, found $v")
            }
        }

        override fun procedureEnd(p: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
            assertEquals(i, vals.size)
        }
    }

    fun trackVarHistory(id: String, vararg vals: Int) {
        vm.addListener(TrackIntVar(id, *vals))
    }

    inner class TrackDoubleVar(val v : IVariableDeclaration<*>, vararg val vals: Double) : IVirtualMachine.IListener {
        var i = 0
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (v.ownerProcedure == procedure && a.target == v) {
                val v = vm.topFrame.variables[a.target]!!.toDouble()
                assertTrue(i < vals.size, "expected not enough")
                assertEquals(v, vals[i], "expected ${vals[i++]}, found $v")
            }
        }

        override fun procedureEnd(p: IProcedureDeclaration, args: List<IValue>, result: IValue?) {
            assertEquals(i, vals.size)
        }
    }
}



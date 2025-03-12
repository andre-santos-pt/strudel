package pt.iscte.strudel

import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import java.io.File


fun main(args: Array<String>) {
    if(args.isEmpty()) {
        System.err.println("at least one Java file has to be provided as argument")
        return
    }

    val files = args.map { File(it) }
    val (found, notfound) = files.partition { it.exists() }

    notfound.forEach { System.err.println("$it not found") }
    val module = Java2Strudel().load(found)
    val main = module.getProcedure { it.id == "main" }
    if(main == null) {
        System.err.println("Main procedure not found")
    }
    else {
        val vm = IVirtualMachine.create()
        vm.execute(main, NULL)
    }
}
package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class Module : ProgramElement(), IModule {
    override val members = mutableListOf<IModuleMember>()

    override fun add(member: IModuleMember) {
        require(!members.contains((member)))
        members.add(member)
    }

    override fun remove(member: IModuleMember) {
        require(members.contains((member)))
        members.remove(member)
    }

    override fun toString(): String {
        var output = ""
        val namespaces = members
            .filter { !it.isForeign }
            .filterIsInstance<IProcedureDeclaration>()
            .filter { it.hasProperty(NAMESPACE_PROP) }
            .groupBy { it.getProperty(NAMESPACE_PROP) as String }

        namespaces.forEach {
            output += "class ${it.key} {\n\t" + it.value.joinToString(separator = "\n\n\t") { "$it" } +"\n}\n\n"
        }

        members.filterIsInstance<IRecordType>().forEach {
            output += it.toString() + "\n\n"
        }
        return output
    }


//    fun loadBuiltInProcedures(clazz: Class<*>) {
//        for (method in clazz.declaredMethods) if (BuiltinProcedureReflective.isValidForBuiltin(
//                this,
//                method
//            )
//        ) procedures.add(
//            BuiltinProcedureReflective(this, method)
//        )
//        for (cons in clazz.constructors) if (BuiltinProcedureReflective.isValidForBuiltin(this, cons)) procedures.add(
//            BuiltinProcedureReflective(this, cons)
//        )
//    }
//
//    fun loadBuiltInProcedures(vararg executable: Executable) {
//        for (m in executable) if (BuiltinProcedureReflective.isValidForBuiltin(this, m)) procedures.add(
//            BuiltinProcedureReflective(this, m)
//        )
//    }

}
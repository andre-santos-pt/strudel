package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.Procedure

/**
 * Mutable
 */


interface IModuleView : IProgramElement {
    val constants: List<IConstantDeclaration>
    val recordTypes: List<IRecordType>
    val procedures: List<IProcedureDeclaration>
}

interface IModule : IModuleView, ITypeProvider {

    val members : MutableCollection<IModuleMember>

    fun add(member: IModuleMember)
    fun remove(member: IModuleMember)

    override val constants: List<IConstantDeclaration>
        get() = members.filterIsInstance<IConstantDeclaration>()

    override val recordTypes: List<IRecordType>
        get() = members.filterIsInstance<IRecordType>()

    override val procedures: List<IProcedureDeclaration>
        get() = members.filterIsInstance<IProcedureDeclaration>()

    operator fun get(procedureId: String) =
        members.find { it is IProcedure && it.id == procedureId } as IProcedure

    val types: Set<IType>
    get()  {
        val list = mutableSetOf<IType>()
        list.addAll(recordTypes)

        procedures.forEach {
            list.addAll((if (it is IProcedure) it.variables else it.parameters).map { it ->
                if (it.type.isReference)
                    (it.type as IReferenceType).resolveTarget()
                else
                    it.type
            })
            list.add(it.returnType)
        }
        return list
    }

    fun getType(id: String): IType = types.find { it.id == id } ?: throw IllegalArgumentException("not found")

    fun getRecordType(id: String): IRecordType
            = members.filterIsInstance<IRecordType>().find { it.id == id } ?: throw IllegalArgumentException(id)
    fun getRecordType(match: (IRecordType) -> Boolean): IRecordType?
        = members.filterIsInstance<IRecordType>().find { match(it) }

    fun getConstant(match: (IConstantDeclaration) -> Boolean) : IConstantDeclaration?
        = members.filterIsInstance<IConstantDeclaration>().find {match(it)}

    fun getProcedure(id: String, namespace: String? = null): IProcedureDeclaration
            = members.filterIsInstance<IProcedureDeclaration>().find { it.id == id && (namespace == null || it.namespace == namespace)}!!

    fun getProcedure(id: String, namespace: String? = null, match: (IProcedureDeclaration) -> Boolean): IProcedureDeclaration =
        members.filterIsInstance<IProcedureDeclaration>().find { it.id == id &&  (namespace == null || it.namespace == namespace) && match(it)}!!

    fun getProcedure(id: String, vararg parameterTypes: IType = arrayOf()): IProcedure =
        members.filterIsInstance<IProcedure>().find {
            it.id == id && it.parameters.size == parameterTypes.size && it.parameters.map { it.type }.zip(parameterTypes).all { it.first == it.second || it.first.isSame(it.second) }
        }!!

    fun getProcedure(match: (IProcedure) -> Boolean): IProcedure?
        = members.filterIsInstance<IProcedure>().find { match(it) }

    fun findProcedures(match: (IProcedure) -> Boolean): List<IProcedure>
        = members.filterIsInstance<IProcedure>().filter { match(it) }

    val namespaces: Set<String>
        get() {
            val set = mutableSetOf<String>()
            constants.forEach { p: IConstantDeclaration -> if (p.namespace != null) set.add(p.namespace!!) }
            recordTypes.forEach { p: IRecordType -> if (p.namespace != null) set.add(p.namespace!!) }
            procedures.forEach { p: IProcedureDeclaration -> if (p.namespace != null) set.add(p.namespace!!) }
            return set
        }

//    fun blendBlockStatements() {
//        procedures.forEach {
//            val blocks = mutableListOf<IBlock>()
//            it.block.accept(object : IBlock.IVisitor {
//                override fun visit(block: IBlock): Boolean {
//                    blocks.add(block)
//                    return super.visit(block)
//                }
//            })
//
//            blocks.forEach {b->
//                val i = b.index
//                b.children.reversed().forEach {c->
//                    c.copyTo(b.parent.block, i)
//                }
//                b.remove()
//            }
//        }
//    }
}

interface IModuleMember : IProgramElement {
    var namespace: String?
        get() = getProperty(NAMESPACE_PROP) as? String
        set(namespace) {
            setProperty(NAMESPACE_PROP, namespace!!)
        }

    fun sameNamespace(e: IModuleMember): Boolean {
        return namespace == e.namespace
    }

    val isForeign: Boolean
        get() = false
}

package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.Procedure

/**
 * Mutable
 */


interface IModuleView : IProgramElement {
    val constants: List<IConstantDeclaration>
    val recordTypes: List<IRecordType>
    val procedures: List<IProcedure>
}

interface IModule : IModuleView {

    val members : MutableCollection<IModuleMember>

    fun add(member: IModuleMember)
    fun remove(member: IModuleMember)

    override val constants: List<IConstantDeclaration>
        get() = members.filterIsInstance<IConstantDeclaration>()

    override val recordTypes: List<IRecordType>
        get() = members.filterIsInstance<IRecordType>()

    override val procedures: List<IProcedure>
        get() = members.filterIsInstance<IProcedure>()

    operator fun get(procedureId: String) =
        members.find { it is IProcedure && it.id == procedureId } as IProcedure

    val types: Set<IType>
    get()  {
        val list = mutableSetOf<IType>()
        list.addAll(recordTypes)

        procedures.forEach {
            list.addAll(it.variables.map { it ->
                if (it.type.isReference)
                    (it.type as IReferenceType).resolveTarget()
                else
                    it.type
            })
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

    fun getProcedure(id: String, namespace: String? = null): IProcedure
            = members.filterIsInstance<IProcedure>().find { it.id == id && (namespace == null || it.namespace == namespace)}!!
    fun getProcedure(match: (IProcedure) -> Boolean): IProcedure?
        = members.filterIsInstance<IProcedure>().find { match(it) }!!

    fun findProcedures(match: (IProcedure) -> Boolean): List<IProcedure>
        = members.filterIsInstance<IProcedure>().filter { match(it) }


    val namespaces: Set<String>
        get() {
            val set = mutableSetOf<String>()
            constants.forEach { p: IConstantDeclaration -> if (p.namespace != null) set.add(p.namespace!!) }
            recordTypes.forEach { p: IRecordType -> if (p.namespace != null) set.add(p.namespace!!) }
            procedures.forEach { p: IProcedure -> if (p.namespace != null) set.add(p.namespace!!) }
            return set
        }

    fun blendBlockStatements() {
        procedures.forEach {
            val blocks = mutableListOf<IBlock>()
            it.block.accept(object : IBlock.IVisitor {
                override fun visit(block: IBlock): Boolean {
                    blocks.add(block)
                    return super.visit(block)
                }
            })

            blocks.forEach {b->
                val i = b.index
                b.children.reversed().forEach {c->
                    c.copyTo(b.parent.block, i)
                }
                b.remove()
            }
        }
    }
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

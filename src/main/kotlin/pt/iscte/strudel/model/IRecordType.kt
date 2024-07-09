package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.RecordAllocation
import pt.iscte.strudel.model.impl.ReferenceType
import pt.iscte.strudel.model.impl.VariableDeclaration

const val RECORD_OVERHEAD = 16

typealias IField = IVariableDeclaration<IRecordType>

/**
 * Mutable
 */
interface IRecordType : IType, IModuleMember {
    val module: IModule?
    val fields: MutableList<IVariableDeclaration<IRecordType>>

    override val bytes: Int
        get() {
            var mem = RECORD_OVERHEAD
            fields.forEach { fieldDeclaration ->
                mem += fieldDeclaration.type.bytes
            }
            return mem
        }

    fun getField(id: String): IVariableDeclaration<IRecordType>? {
        for (f in fields) if (id == f.id) return f
        return null
    }

    operator fun get(id: String): IVariableDeclaration<IRecordType> =
        fields.find { it.id == id }!!

    fun addField(type: IType, configure: IField.() -> Unit = {}): IField

    fun removeField(f: IVariableDeclaration<IRecordType>)

    fun heapAllocation(): IRecordAllocation
    override val defaultExpression: IExpression
        get() = NULL_LITERAL

    override val isUnbound: Boolean
        get() = this is UnboundRecordType
}

class UnboundRecordType(
    id: String
): IRecordType {
    init {
        setProperty(ID_PROP, id)
    }

    override val module: IModule? = null
    override val fields: MutableList<IVariableDeclaration<IRecordType>> = mutableListOf()

    override fun addField(type: IType, configure: (IField) -> Unit): IField {
        TODO("Not yet implemented")
    }

    override fun removeField(`var`: IVariableDeclaration<IRecordType>) {
        TODO("Not yet implemented")
    }

    override fun heapAllocation(): IRecordAllocation {
        TODO("Not yet implemented")
    }

    override fun reference(): IReferenceType {
        TODO("Not yet implemented")
    }

    override fun cloneProperties(e: IProgramElement) {
        TODO("Not yet implemented")
    }

    override fun setProperty(key: String, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getProperty(key: String): Any {
        TODO("Not yet implemented")
    }

    override val bytes: Int
        get() = TODO("Not yet implemented")

}

class HostRecordType(
    qualifiedName: String
): IRecordType {
    val type = Class.forName(qualifiedName)


    override val module: IModule? = null
    override val fields: MutableList<IVariableDeclaration<IRecordType>> = mutableListOf()

    override fun addField(type: IType, configure: (IField) -> Unit): IField {
       throw UnsupportedOperationException()
    }

    override fun removeField(f: IVariableDeclaration<IRecordType>) {
        throw UnsupportedOperationException()
    }

    override fun heapAllocation(): IRecordAllocation {
        return RecordAllocation(this)
    }

    override fun cloneProperties(e: IProgramElement) {
        throw UnsupportedOperationException()
    }

    override fun setProperty(key: String, value: Any?) {
        throw UnsupportedOperationException()
    }

    override fun getProperty(key: String): Any {
        if(key == ID_PROP)
            return type.name
        else
            throw UnsupportedOperationException()
    }

    override fun toString(): String = type.name

    override fun isSame(e: IProgramElement): Boolean {
        return e is HostRecordType && type == e.type
    }

    override val bytes: Int
        get() = RECORD_OVERHEAD // TODO

}
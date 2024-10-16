package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class RecordType(override val module: IModule) : ProgramElement(), IRecordType {
    override val fields: MutableList<IVariableDeclaration<IRecordType>> = mutableListOf()

    private var referenceType: IReferenceType? = null
    private var arrayType: IArrayType? = null

    init {
        module.add(this)
    }

    override fun addField(type: IType, configure: (IField) -> Unit): IField {
        val f = VariableDeclaration<IRecordType>(this, type)
        fields.add(f)
        configure(f)
        return f
    }

    override fun removeField(f: IVariableDeclaration<IRecordType>) {
        fields.remove(f)
    }

    override fun heapAllocation(): IRecordAllocation {
        return RecordAllocation(this)
    }

    override fun array(): IArrayType {
        if (arrayType == null)
            arrayType = ArrayType(reference())
        return arrayType!!
    }

    override fun reference(): IReferenceType {
        if (referenceType == null)
            referenceType = ReferenceType(this)
        return referenceType!!
    }

    override fun toString(): String = "class $id {\n " + fields.joinToString(separator = "\n") {
        "\t${it.type.id} ${it.id};"
    } + "\n}"

}
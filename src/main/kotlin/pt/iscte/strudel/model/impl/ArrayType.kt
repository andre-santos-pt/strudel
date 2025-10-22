package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal class ArrayType(override val componentType: IType) : ProgramElement(), IArrayType {

    private var referenceType: IReferenceType? = null
    private var arrayType: IArrayType? = null

    init {
        id = componentType.id + "[]"
    }

    override fun heapAllocation(dimensions: List<IExpression?>): IArrayAllocation {
        return ArrayAllocation(componentType, dimensions)
    }

    override fun heapAllocationWith(elements: List<IExpression>): IArrayAllocation {
        return PredefinedArrayAllocation(componentType, elements)
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

    override fun toString(): String = id!!
}
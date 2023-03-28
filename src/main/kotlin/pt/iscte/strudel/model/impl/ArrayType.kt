package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal class ArrayType(override val componentType: IType) : ProgramElement(), IArrayType {

    init {
        id = componentType.id + "[]"
    }

    val defaultValue: Any?
        get() = null

    val memoryBytes: Int
        get() = 8

    override fun reference(): IReferenceType {
        return ReferenceType(this)
    }

    override fun heapAllocation(dimensions: List<IExpression>): IArrayAllocation {
        return ArrayAllocation(componentType, dimensions)
    }

    override fun heapAllocationWith(elements: List<IExpression>): IArrayAllocation {
        return PredefinedArrayAllocation(componentType, elements)
    }

    override fun toString(): String = id!!
}
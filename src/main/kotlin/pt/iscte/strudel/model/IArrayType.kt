package pt.iscte.strudel.model



interface IArrayType : IType {
    val componentType: IType

    override val defaultExpression: IExpression
        get() = NULL_LITERAL

    val rootComponentType: IType get() =
        if(componentType.isArrayReference)
            ((componentType as IReferenceType).target as IArrayType).componentType
    else
        componentType

    override fun isSame(e: IProgramElement): Boolean {
        return e is IArrayType && componentType.isSame(e.componentType)
    }

    fun heapAllocation(dimensions: List<IExpression>): IArrayAllocation

    fun heapAllocation(singleDimension: IExpression): IArrayAllocation
        = heapAllocation(listOf(singleDimension))

    fun heapAllocationWith(elements: List<IExpression>): IArrayAllocation
    fun heapAllocationWith(vararg elements: IExpression): IArrayAllocation {
        return heapAllocationWith(listOf(*elements))
    }
}
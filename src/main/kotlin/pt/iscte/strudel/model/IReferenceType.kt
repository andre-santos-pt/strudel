package pt.iscte.strudel.model


interface IReferenceType : IType {
    val target: IType
    override fun isSame(e: IProgramElement): Boolean {
        return e is IReferenceType && target.isSame(e.target)
    }

    fun resolveTarget(): IType? {
        var t = target
        while (t is IReferenceType) t = t.target
        return t
    }


    override val defaultExpression: IExpression
        get() = NULL_LITERAL
}
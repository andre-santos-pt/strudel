package pt.iscte.strudel.model


interface IControlStructure : IBlockElement, IBlockHolder {
    override var parent: IBlock
    var guard: IExpression
}

interface ISelection : IControlStructure {
    val alternativeBlock: IBlock?
    fun createAlternativeBlock(content: (IBlock) -> Unit = {}): IBlock
    fun hasAlternativeBlock() = alternativeBlock != null
    fun deleteAlternativeBlock()
}

interface ILoop : IControlStructure
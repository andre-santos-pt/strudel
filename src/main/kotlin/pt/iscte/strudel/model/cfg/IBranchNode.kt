package pt.iscte.strudel.model.cfg

import pt.iscte.strudel.model.IControlStructure

interface IBranchNode : INode {
    val alternative: INode?
    fun setBranch(node: INode)
    fun hasBranch(): Boolean
    override val element: IControlStructure
}
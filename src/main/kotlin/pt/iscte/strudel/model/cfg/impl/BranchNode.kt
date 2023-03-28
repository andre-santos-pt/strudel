package pt.iscte.strudel.model.cfg.impl

import pt.iscte.strudel.model.IControlStructure
import pt.iscte.strudel.model.cfg.IBranchNode
import pt.iscte.strudel.model.cfg.INode

class BranchNode (override val element: IControlStructure) : Node(), IBranchNode {
    override var alternative: INode? = null
        private set

    override fun setBranch(node: INode) {
        require(alternative == null)
        alternative = node
    }

    override fun hasBranch(): Boolean {
        return alternative != null
    }

    override fun toString(): String {
        val next = next
        var s = ""
        var nextText = "(NO NEXT!)"
        if (next != null) nextText = if (next.isExit) next.toString() else text(next.element)
        s += "! " + text(element.guard)  + " >>>> " + nextText + "\n"
        var altText = "(NO BRANCH!)"
        if (alternative != null) altText =
            if (alternative!!.isExit) alternative.toString() else text(alternative!!.element)
        s += text(element.guard) + " >..> " + altText
        return s
    }

    override fun isEquivalentTo(node: INode): Boolean {
        if (node !is IBranchNode) return false
        val n = node // checked by super
        return super.isEquivalentTo(node) &&
                (alternative == null && n.alternative == null || alternative != null &&
                        alternative!!.element == null && n.alternative!!.element == null ||
                        alternative != null && alternative!!.element != null && alternative!!.element == n.alternative!!.element)
    }
}
package pt.iscte.strudel.model.cfg.impl

import pt.iscte.strudel.model.IStatement
import pt.iscte.strudel.model.cfg.IStatementNode

class StatementNode (override val element: IStatement) : Node(), IStatementNode {
    override fun toString(): String {
        val next = next
        return if (next == null) text(element)+ " >>>> (NO NEXT!)"
        else text(element) + " >>>> " + if (!next.isExit) text(next.element) else next.toString()
    }
}
package pt.iscte.strudel.model.cfg

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IStatement

interface IStatementNode : INode {
    override val element: IStatement
}
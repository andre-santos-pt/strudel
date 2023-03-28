package pt.iscte.strudel.model.cfg.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.cfg.IStatementNode
import pt.iscte.strudel.model.cfg.IBranchNode
import pt.iscte.strudel.model.cfg.INode
import pt.iscte.strudel.model.util.contains
import pt.iscte.strudel.model.util.find


class ControlFlowGraph(override var procedure: IProcedure) : IControlFlowGraph {
    override val entryNode: INode = Node()
    override val exitNode: INode = Node()
    override val nodes: List<INode> = mutableListOf(entryNode, exitNode)


    internal fun buildGraph() {
        if(procedure.block.isEmpty) {
            entryNode.next = exitNode
        }
        else {
            val cfgGenerator = CfgGenerator(this)
            procedure.accept(cfgGenerator)

            //if (procedure.returnType == VOID) {
            for (n in nodes) {
                if (n.isEntry || n.isExit) continue
                if (n.next == null) n.next = exitNode
                if (n is IBranchNode && !n.hasBranch()) n.setBranch(exitNode)
            }
            //}
        }
    }

    override fun newStatement(statement: IStatement): IStatementNode {
        require(procedure.contains(statement))
        val n: IStatementNode = StatementNode(statement)
        (nodes as MutableList).add(n)
        return n
    }

    override fun newBranch(structure: IControlStructure): IBranchNode {
        require(procedure.contains(structure))
        val n: IBranchNode = BranchNode(structure)
        (nodes as MutableList).add(n)
        return n
    }

    override fun toString(): String {
        return nodes.joinToString("\n") { it.toString() }
    }
}
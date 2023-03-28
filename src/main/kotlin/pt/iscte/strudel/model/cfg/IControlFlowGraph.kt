package pt.iscte.strudel.model.cfg

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.impl.ControlFlowGraph

fun IProcedure.createCFG() = IControlFlowGraph.create(this)

interface IControlFlowGraph {
    val procedure: IProcedure
    val nodes: List<INode>
    val entryNode: INode
    val exitNode: INode

    val isEmpty : Boolean get() = nodes.size == 2

    fun newStatement(statement: IStatement): IStatementNode
    fun newStatement(previous: INode, statement: IStatement): IStatementNode {
        val s = newStatement(statement)
        previous.next = s
        return s
    }

    fun newBranch(expression: IControlStructure): IBranchNode
    fun newBranch(previous: INode, expression: IControlStructure): IBranchNode {
        val b = newBranch(expression)
        previous.next = b
        return b
    }


    fun display() {
        nodes.forEach { println(it) }
    }

    class Path {
        val nodes: MutableList<INode> = ArrayList()
        fun addNode(node: INode) {
            nodes.add(node)
        }

        fun existsInPath(node: INode?): Boolean {
            return if (node != null) nodes.contains(node) else false
        }
    }

    fun generateSubCFG(source: INode, destination: INode): List<INode> {
        val nodes: MutableList<INode> = mutableListOf()
        nodes.add(entryNode)
        var start = false
        for (node in nodes) {
            if (node == source) start = true
            if (start) nodes.add(node)
            if (node == destination) start = false
        }
        nodes.add(exitNode)
        return nodes
    }

    fun usedOrChangedBetween(source: INode, destiny: INode, variable: IVariableDeclaration<*>): Boolean {
        val paths = pathsBetweenNodes(source, destiny)
        if (paths.isEmpty()) return true
        for (path in paths) {
            path.nodes.removeAt(0) // remove the beginning node.
            val end: INode = path.nodes.removeAt(path.nodes.size - 1)
            for (node in path.nodes) {
                if (node.element is IArrayElementAssignment
                    && ((node.element as IArrayElementAssignment).arrayAccess.target.isSame(variable.expression())
                            || (node.element as IArrayElementAssignment).expression.includes(variable))
                ) return true else if (node.element is IVariableAssignment
                    && ((node.element as IVariableAssignment).target === variable
                            || (node.element as IVariableAssignment).expression.includes(variable))
                ) return true else if (node.element is IProcedureCall) for (argument in (node.element as IProcedureCall).arguments) if (argument.includes(
                        variable
                    )
                ) return true else if ((node.element is ISelection || node.element is ILoop)
                    && (node.element as IControlStructure).guard.includes(variable)
                ) {
                    return true
                }
            }
            if (end.element is IVariableAssignment
                && (((end.element as IVariableAssignment).target.isSame(variable) || (end.element as IVariableAssignment).target.isSame(
                    variable.expression()
                ))
                        && (end.element as IVariableAssignment).expression.includes(variable))
            ) return true
        }
        return false
    }

    /**
     * Goes through the ControlFlowGraph nodes searching for all the possible paths between the supplied source and destination nodes.
     * @param source The source node where the search will start.
     * @param destination The destination node where the search will start.
     * @return List with all the possible paths between the source and destination nodes.
     */
    fun pathsBetweenNodes(source: INode, destination: INode): List<Path> {
        val paths: MutableList<Path> = ArrayList()
        pathsBetweenNodes(paths, source, destination)
        return paths
    }

    fun reachability(startNode: INode = entryNode): List<INode> {
        val list = mutableListOf<INode>()
        reachability(startNode, list)
        return list
    }

    fun deadNodes(): List<INode> {
        val reachability = reachability()
        return nodes.filter { !reachability.contains(it) }
    }

    fun isEquivalentTo(cfg: IControlFlowGraph): Boolean {
        return checkEquivalentNodes(reachability(), cfg.reachability()) &&
                checkEquivalentNodes(deadNodes(), cfg.deadNodes())
    }

    fun isValid() : Boolean {
        nodes.forEach {
            if(it != exitNode && it.next == null)
                return false
            if(it is IBranchNode && it.alternative == null)
                return false
        }
        return true
    }

    companion object {
        fun create(procedure: IProcedure): IControlFlowGraph {
            val cfg = ControlFlowGraph(procedure)
            cfg.buildGraph()
            return cfg
        }

        fun createEmpty(procedure: IProcedure) = ControlFlowGraph(procedure)

        private fun pathsBetweenNodes(paths: MutableList<Path>, source: INode, destination: INode) {
            require(!source.isEquivalentTo(destination)) { "Source and destination nodes can't be the same!" }
            val path = Path()
            path.addNode(source)
            if (source is IBranchNode && source.hasBranch())
                pathFinder(paths, path, source.alternative!!, destination)
            if (source.next != null && !source.next!!.isExit)
                pathFinder(paths, path, source.next!!, destination)
        }

        private fun pathFinder(paths: MutableList<Path>, path: Path, source: INode, destination: INode) {
            var beenHere = false
            if (!path.existsInPath(source)) path.addNode(source) else beenHere = true
            if (source.isEquivalentTo(destination)) {
                paths.add(path)
                return
            }
            if (!beenHere && source is IBranchNode && source.hasBranch()) {
                val replica = Path()
                path.nodes.forEach { replica.addNode(it) }
                pathFinder(paths, replica, source.alternative!!, destination)
            }
            if (source.next != null && !source.next!!.isExit)
                pathFinder(paths, path, source.next!!, destination)
        }

        fun reachability(n: INode, list: MutableList<INode>) {
            if (!list.contains(n)) {
                list.add(n)
                if (n.next != null) {
                    if (n is IBranchNode) reachability(n.alternative!!, list)
                    reachability(n.next!!, list)
                }
            }
        }

        fun checkEquivalentNodes(a: List<INode>, b: List<INode>): Boolean {
            if (a.size != b.size) return false
            for (i in a.indices) if (!a[i].isEquivalentTo(b[i])) return false
            return true
        }
    }
}
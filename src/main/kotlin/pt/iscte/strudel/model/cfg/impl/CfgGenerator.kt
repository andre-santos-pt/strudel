package pt.iscte.strudel.model.cfg.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.IControlFlowGraph
import pt.iscte.strudel.model.cfg.INode
import pt.iscte.strudel.model.cfg.IBranchNode
import pt.iscte.strudel.model.cfg.IStatementNode
class CfgGenerator(val cfg: IControlFlowGraph) : IBlock.IVisitor {

    inline fun <T> ArrayDeque<T>.peek() = firstOrNull() // returns Unit

    inline fun <T> ArrayDeque<T>.pop() : T = removeFirst()

    private val branchTypeDeque: ArrayDeque<BranchType> = ArrayDeque()

    private var lastVisitedNode: INode = cfg.entryNode
    private var lastVisitedSelectionBranchNode: INode? = null
    private var lastVisitedLoopBranchNode: INode? = null
    private var lastVisitedBreakNode: IStatementNode? = null

    private val selectionBranchStack = ArrayDeque<IBranchNode>()
    private val loopBranchStack = ArrayDeque<IBranchNode>()
    private val breakStack = ArrayDeque<IStatementNode>()

    private val selectionBranchArchive = ArrayDeque<IBranchNode>()
    private val orphans: MutableMap<IBranchNode, MutableList<INode>> = mutableMapOf()

    init {
        branchTypeDeque.add(BranchType.ROOT)
    }

    override fun visitAny(element: IBlockElement) {
        if (element is IStatement) {
            val statementNode = cfg.newStatement(element)

            // In case that this is the first assignment in the procedure.
            if (lastVisitedNode.isEntry) lastVisitedNode.next = statementNode
            setLastBranchNode(statementNode)
            handleCurrentNode(statementNode)
            handleLastVisitedBranchNodes(statementNode)
            if (element is IBreak) breakStack.addFirst(statementNode)
            if (element is IContinue && !loopBranchStack.isEmpty()) statementNode.next = loopBranchStack.peek()
            if (element is IReturn) statementNode.next = cfg.exitNode
            lastVisitedNode = statementNode
        } else if (element is IControlStructure) {
            val branchNode = cfg.newBranch(element)
            if (lastVisitedNode.isEntry) lastVisitedNode.next = branchNode
            setLastBranchNode(branchNode)
            handleCurrentNode(branchNode)
            handleLastVisitedBranchNodes(branchNode)
            if (element is ISelection) {
                branchTypeDeque.addFirst(BranchType.SELECTION)
                selectionBranchStack.addFirst(branchNode)
            } else if (element is ILoop) {
                branchTypeDeque.addFirst(BranchType.LOOP)
                loopBranchStack.addFirst(branchNode)
            }
            lastVisitedNode = branchNode
        }
    }

    override fun endVisit(selection: ISelection) {
        val selectionNode = selectionBranchStack.pop()
        branchTypeDeque.pop()

        // There are orphans that match this selection, that need to be added to the previous selection orphan list.
        if (orphans[selectionBranchArchive.peek()] != null) {
            if (!orphans.containsKey(selectionNode))
                orphans[selectionNode] = ArrayList()

            orphans[selectionNode]!!.addAll(orphans[selectionBranchArchive.peek()]!!)

            orphans.remove(selectionBranchArchive.peek())
            selectionBranchArchive.pop()
        }
        lastVisitedSelectionBranchNode = selectionNode
        selectionBranchArchive.addFirst(selectionNode)
    }

    override fun visitAlternative(selection: ISelection): Boolean {
        branchTypeDeque.addFirst(BranchType.ALTERNATIVE)
        return true
    }

    override fun endVisitAlternative(selection: ISelection) {
        branchTypeDeque.pop()
    }

    override fun endVisit(loop: ILoop) {
        val loopNode = loopBranchStack.pop()
        branchTypeDeque.pop()
        if (lastVisitedNode.next == null) lastVisitedNode.next = loopNode
        dealWithEventualOrphans(loopNode)
        if (!breakStack.isEmpty()) lastVisitedBreakNode = breakStack.pop()
        lastVisitedLoopBranchNode = loopNode
    }

    private fun handleLastVisitedBranchNodes(newNode: INode) {
        if (lastVisitedSelectionBranchNode != null && lastVisitedSelectionBranchNode!!.next == null) {
            lastVisitedSelectionBranchNode!!.next = newNode
            lastVisitedSelectionBranchNode = null
        }
        if (lastVisitedLoopBranchNode != null && lastVisitedLoopBranchNode!!.next == null) {
            lastVisitedLoopBranchNode!!.next = newNode
            lastVisitedLoopBranchNode = null
        }
    }

    private fun dealWithEventualOrphans(node: INode) {
        if (orphans.isNotEmpty() && orphans[lastVisitedSelectionBranchNode] != null) {
            orphans[lastVisitedSelectionBranchNode]!!
                .forEach { n: INode -> if (n.next == null) n.next = node }
            orphans.remove(lastVisitedSelectionBranchNode)
        }
    }

    private fun setLastBranchNode(node: INode) {
        val lastSelectionBranch = selectionBranchStack.peek()
        val lastLoopBranch = loopBranchStack.peek()
        // Means that we are in an if block. The last branch node is checked and if it doesn't have an, 
        // alternative node, it is set as the current node that is being visited.
        if (branchTypeDeque.peek() == BranchType.SELECTION && lastSelectionBranch != null && lastSelectionBranch.element != null && !lastSelectionBranch.hasBranch()) {
            lastSelectionBranch.setBranch(node)
        } else if (branchTypeDeque.peek() == BranchType.ALTERNATIVE && lastSelectionBranch != null && lastSelectionBranch.element != null && lastSelectionBranch.next == null) {
            lastSelectionBranch.next = node
        } else if (branchTypeDeque.peek() == BranchType.LOOP && lastLoopBranch != null && lastLoopBranch.element != null && !lastLoopBranch.hasBranch()) {
            lastLoopBranch.setBranch(node)
        }
    }

    private fun handleCurrentNode(node: INode) {
        if (lastVisitedBreakNode != null) {
            lastVisitedBreakNode!!.next = node
            lastVisitedBreakNode = null
        }

        // Only set as next if the nodes are right next to each other.
        if (lastVisitedNode != null && lastVisitedNode.next == null) {
            val lastElement: IBlockElement? = if (lastVisitedNode is BranchNode) {
                (lastVisitedNode as IBranchNode).element
            } else {
                lastVisitedNode.element as IBlockElement?
            }
            val newElement: IBlockElement? = if (node is BranchNode) {
                (node as IBranchNode).element
            } else {
                node.element as IBlockElement?
            }
            if (lastElement != null && newElement != null && (lastElement.parent.isSame(newElement.parent)
                        || (newElement.parent as IBlock).isStatementBlock)) // case of block inside block
                        {
                lastVisitedNode.next = node
            } else if (!selectionBranchStack.isEmpty()) {
                val branch = selectionBranchStack.peek()
                if (!orphans.containsKey(branch))
                    orphans[branch!!] = ArrayList()

                orphans[branch]!!.add(node)

                // this means that the newNode is a selection orphan.
            }
        }
        // Checks if there are orphan nodes from previous branches, who's next node needs to be assigned.
        dealWithEventualOrphans(node)
    }
}
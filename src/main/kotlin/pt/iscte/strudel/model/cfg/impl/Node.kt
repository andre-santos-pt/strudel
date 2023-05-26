package pt.iscte.strudel.model.cfg.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.cfg.INode
import pt.iscte.strudel.model.dsl.Translator

open class Node : INode {
    override var next: INode? = null
        set(value) {
            //check(field == null)
            field = value
            ((next as Node).incoming as MutableSet).add(this)
        }

    override val incoming: Set<INode> = HashSet()

    override val element: IProgramElement? = null

    internal  fun text(e: IProgramElement?) =
        when(e){
            is ILoop -> "loop(" + e.guard.toString() +")"
            is ISelection -> "if(" + e.guard.toString() +")"
            is IStatement -> e.toString()
            is IExpression -> e.toString()
            else -> e.toString()
        }


    override fun toString(): String {

        return if (isEntry) "ENTRY -> " + (if (next != null) text(next!!.element) else "NULL NODE")
        else if (isExit) "EXIT"
        else super.toString()
    }

    override val isEntry: Boolean
        get() = element == null && incoming.isEmpty()

    override val isExit: Boolean
        get() = element == null && next == null

    override fun isEquivalentTo(node: INode): Boolean {
        return (this::class == node::class &&
                (element == null && node.element == null ||
                        element != null && element == node.element)
                &&
                (next == null && node.next == null || next != null && next!!.element == null &&
                        node.next!!.element == null || next != null && next!!.element != null && next!!.element == node.next!!.element))
    }
}

package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.Translator

internal class Block(override val parent: IBlockHolder,
            addToParent: Boolean,
            index: Int = -1, vararg flags: String)
    : ProgramElement(*flags), IBlock {
    private val contents: MutableList<IBlockElement> = mutableListOf()

    init {
        require(!addToParent || parent is Block)
        if (addToParent) (parent as Block).add(this, index)
    }

    override val children: List<IBlockElement>
        get() = contents

    override val isEmpty: Boolean
        get() = children.all { it is Block && it.isEmpty }

    override val size: Int
        get() = children.size



    override fun add(e: IBlockElement, index: Int) {
        //require(e.parent === this)
        contents.add(if(index == -1) contents.size else index, e)

    }

    override fun remove(e: IBlockElement) {
        require(children.contains(e))
        contents.remove(e)
    }

    override fun moveTo(e: IBlockElement, index: Int) {
        require(children.contains(e))
        require(index in 0 .. children.lastIndex)
        contents.remove(e)
        contents.add(index, e)
    }

    override fun moveAfter(e: IBlockElement, target: IBlockElement) {
        require(children.contains(e))
        require(children.contains(target))
        contents.remove(e)
        val i = contents.indexOf(target)
        contents.add(i+1, e)
    }

    fun addLooseBlock(parent: IBlockHolder): IBlock {
        return Block(parent, false, -1)
    }

    override val procedure: IProcedure?
        get() = if (parent is Procedure) parent as IProcedure else if (parent == null) null else if (parent is ControlStructure) parent.parent.procedure else (parent as Block).procedure


    override fun copyTo(newParent: IBlockHolder, index: Int): IBlock {
        val copy = Block(newParent.block, true, index, *flags.toTypedArray())
        children.forEach {
            it.copyTo(copy)
        }
        return copy

    }

    override fun toString(): String = "{\n" + children.joinToString(separator = "\n") { "$it" } + "\n" + tabs(parent).dropLast(1) + "}"
}
package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal sealed class ControlStructure(
    override var parent: IBlock,
    override var guard: IExpression,
    index: Int,
    vararg flags: String
) : ProgramElement(*flags), IControlStructure {

    override lateinit var block: IBlock

    init {
        block = (parent as Block).addLooseBlock(this)
        parent.add(this, index)
        guard.setProperty("IControlStructure", this)
    }

}

internal class Selection(parent: IBlock, guard: IExpression, index: Int = -1, hasAlternative: Boolean = false) :
    ControlStructure(parent, guard, index), ISelection {

    override var alternativeBlock: IBlock? = null

    init {
        alternativeBlock = if (hasAlternative) (parent as Block).addLooseBlock(this) else null
        if (hasAlternative) setFlag("ELSE")
    }

    override fun createAlternativeBlock(content: (IBlock) -> Unit): IBlock {
        alternativeBlock = (parent as Block).addLooseBlock(this)
        setFlag("ELSE")
        content(alternativeBlock!!)
        return alternativeBlock as IBlock
    }

    override fun deleteAlternativeBlock() {
        alternativeBlock = null
        unsetFlag("ELSE")
    }

    override fun copyTo(newParent: IBlockHolder, index: Int): ISelection {
        val copy = Selection(newParent.block, guard, index)
        block.children.forEach {
            it.copyTo(copy)
        }
        alternativeBlock?.let {elseBlock ->
            val elze = copy.createAlternativeBlock()
            elseBlock.children.forEach {
                it.copyTo(elze)
            }
        }
        copy.cloneProperties(this)
        return copy
    }

    override fun toString(): String = tabs(parent) + "if($guard) $block" + if(alternativeBlock == null) "" else "else $alternativeBlock"
}

internal class Loop(parent: IBlock, guard: IExpression, index: Int = -1, vararg flags: String)
    : ControlStructure(parent, guard, index, *flags), ILoop {

    override fun copyTo(newParent: IBlockHolder, index: Int): ILoop {
        val copy = Loop(newParent.block, guard, index)
        block.children.forEach {
            it.copyTo(copy)
        }
        copy.cloneProperties(this)
        return copy
    }

    override fun toString(): String = tabs(parent) + "while($guard) $block"
}


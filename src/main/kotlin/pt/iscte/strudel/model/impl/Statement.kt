package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal sealed class Statement(override val parent: IBlock, vararg flags: String)
    : ProgramElement(*flags), IStatement {

    fun addToParent(index: Int) {
        if (parent != null) {
            val block = parent as Block
            if(index == -1)
                block.add(this)
            else
                block.add(this, index)
        }
    }

    override fun remove() {
        (parent as Block?)!!.remove(this)
    }
}



internal class VariableAssignment(
    override var parent: IBlock,
    override var target: IVariableDeclaration<*>,
    override var expression: IExpression,
    index: Int = -1,
    vararg flags: String
) : Statement(parent, *flags), IVariableAssignment {
    init {
        addToParent(index)
    }

    override fun toString() =  tabs(parent) +  "${target.id?:"\$${ownerProcedure.variables.indexOf(target)}"} = $expression;"

    override fun copyTo(newParent: IBlockHolder, index: Int): IVariableAssignment {
       val copy = VariableAssignment(newParent.block, target, expression, index)
        copy.cloneProperties(this)
        return copy
    }
}

internal class RecordFieldAssignment(
    override var parent: IBlock,
    override var target: ITargetExpression,
    override val field: IVariableDeclaration<IRecordType>,
    override var expression: IExpression,
    index: Int = -1
) : Statement(parent), IRecordFieldAssignment {

    init {
        addToParent(index)
    }

    override fun toString() =  tabs(parent) +  "$target.${field.id} = $expression;"

    override fun copyTo(newParent: IBlockHolder, index: Int): IRecordFieldAssignment {
        val copy = RecordFieldAssignment(newParent.block, target, field, expression, index)
        copy.cloneProperties(this)
        return copy
    }
}

internal class ArrayElementAssignment(
    override var parent: IBlock,
    val target: ITargetExpression,
    override var expression: IExpression,
    index: Int = -1,
    val arrayIndex: IExpression,
) : Statement(parent), IArrayElementAssignment {

    override var arrayAccess: IArrayAccess = ArrayAccess(target, arrayIndex)

    init {
        addToParent(index)
    }

    override fun toString() = tabs(parent) + "$arrayAccess = $expression;"

    override fun copyTo(newParent: IBlockHolder, index: Int): IArrayElementAssignment {
        val copy = ArrayElementAssignment(newParent.block, target, expression, index, arrayIndex)
        copy.cloneProperties(this)
        return copy
    }
}


internal class Return(
    override var parent: IBlock,
    override var expression: IExpression? = null,
    index: Int = -1,
    override var isError: Boolean = false)
    : Statement(parent), IReturn {

    init {
        addToParent(index)
    }

    override fun toString() = tabs(parent) + "return ${expression?: ""};"

    override fun copyTo(newParent: IBlockHolder, index: Int): IReturn {
        val copy = Return(newParent.block, expression, index, isError)
        copy.cloneProperties(this)
        return copy
    }

}

internal class Break(parent: IBlock, index: Int = -1) : Statement(parent), IBreak {
    init {
        addToParent(index)
    }

    override fun toString() =  tabs(parent) +  "break;"

    override fun copyTo(newParent: IBlockHolder, index: Int): IBreak =
       Break(newParent.block, index)

}


internal class Continue(parent: IBlock, index: Int = -1) : Statement(parent), IContinue {
    init {
        addToParent(index)
    }

    override fun toString() =  tabs(parent) +  "continue;"

    override fun copyTo(newParent: IBlockHolder, index: Int): IContinue =
        Continue(newParent.block, index)
}


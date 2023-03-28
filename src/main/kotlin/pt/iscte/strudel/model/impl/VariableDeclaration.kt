package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class VariableDeclaration<T:IProgramElement>(
    override val owner: T,
    override val type: IType,
    index: Int = -1,
    vararg flags: String,
)
    : ProgramElement(*flags), IVariableDeclaration<T> {

    init {
        if(owner is Block)
            owner.add(this, index)
    }

    override val parent: IBlockHolder get() = owner as IBlockHolder

    override fun copyTo(newParent: IBlockHolder, index: Int): IVariableDeclaration<T> {
        val copy = VariableDeclaration(newParent as T, type, index, *flags.toTypedArray())
        copy.cloneProperties(this)
        return copy
    }

    override fun toString(): String =
        when(owner) {
            is Block -> (tabs(owner) + "${type.id} ${id ?: "\$${ownerProcedure.variables.indexOf(this)}"};")
            is IProcedureDeclaration ->  "${type.id} ${id ?: "\$${ownerProcedure.variables.indexOf(this)}"}"
            else ->  "$type ${id ?: "\$v"}"
        }
}

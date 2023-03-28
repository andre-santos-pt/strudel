package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.ProgramElement
import pt.iscte.strudel.model.impl.RecordFieldExpression
import pt.iscte.strudel.model.impl.VariableExpression



interface IVariableDeclaration<T: IProgramElement>: IBlockElement {

    val owner : T
    val type: IType

    val isParameter get() = owner is IProcedureDeclaration
    val isLocal get() = owner is IBlock
    val isField get() = owner is IRecordType

    val isUnbound: Boolean get() = this is UnboundVariableDeclaration<*>

    fun expression(): ITargetExpression = VariableExpression(this)

    fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression = RecordFieldExpression(this.expression(),  field)

    operator fun get(fieldId: String):IRecordFieldExpression = RecordFieldExpression(this.expression(), (type as IRecordType)[fieldId])
}

internal class UnboundVariableDeclaration<T: IProgramElement>(id: String, override val owner: T) : ProgramElement(), IVariableDeclaration<T> {
    init {
      setProperty(ID_PROP, id)
    }

    override val type: IType
        get() = UnboundType()
    override val parent: IBlockHolder
        get() = owner as IBlockHolder

    override fun copyTo(newParent: IBlockHolder, index: Int): IVariableDeclaration<T> =
        UnboundVariableDeclaration(id ?: "", newParent.block  as T)
}
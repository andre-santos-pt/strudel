package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal class RecordFieldExpression(
    override val target: ITargetExpression,
    override val field: IVariableDeclaration<IRecordType>
    ) : Expression(), IRecordFieldExpression {

    override val parts: List<IExpression>
        get() = listOf(target)

    override val type: IType
        get() = this.field.type

    override fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression {
        return RecordFieldExpression(this, field)
    }

    override fun element(index: IExpression): IArrayAccess {
        return ArrayAccess(this, index)
    }

    override fun toString(): String = "$target.${field.id}"
}
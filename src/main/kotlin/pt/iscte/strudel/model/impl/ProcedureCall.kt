package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class ProcedureCall(
    override var parent: IBlockHolder,
    override var procedure: IProcedureDeclaration,
    index: Int = -1,
    override val arguments: List<IExpression>
) :
    Expression(), IProcedureCall, IProcedureCallExpression {

    init {
        if (parent != NullBlock)
            (parent as Block).add(this, index)
    }

    override val expressionParts: List<IExpression>
        get() = arguments

    override val type: IType
        get() = procedure.returnType


    override val parts: List<IExpression>
        get() = arguments

    override fun copyTo(newParent: IBlockHolder, index: Int): IProcedureCall =
        ProcedureCall(newParent.block, procedure, index, arguments)

    override fun toString(): String =
        (if (parent != NullBlock) tabs(parent) else "") +
                (if (procedure.namespace != null) procedure.namespace + "." else "") +
                "${procedure.id}(" + arguments.joinToString(", ") { "$it" } + ")" +
                (if (parent != NullBlock) ";" else "")
}
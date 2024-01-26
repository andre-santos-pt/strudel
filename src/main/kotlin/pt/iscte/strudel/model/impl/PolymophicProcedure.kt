package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal class PolymophicProcedure(
    override val module: IModule,
    override var namespace: String?,
    override var id: String?,
    override val returnType: IType
) : ProgramElement(), IPolymorphicProcedure {

    override val parameters: List<IParameter> = mutableListOf()

    init {
        module.members.add(this)
    }

    override fun addParameter(type: IType): IParameter {
        val param: IParameter = VariableDeclaration(this, type)
        (parameters as MutableList).add(param)
        return param
    }

    override fun expression(args: List<IExpression>): IProcedureCallExpression {
        return ProcedureCall(NullBlock, this, -1, args.toMutableList())
    }
}
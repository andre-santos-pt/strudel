package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*

internal open class Procedure(
    override val module: IModule,
    override var returnType: IType
) : ProgramElement(), IProcedure {

    init {
        module.members.add(this)
    }

    override var block: IBlock = Block(this, false)

    override val parameters: List<IVariableDeclaration<IProcedureDeclaration>> =
        mutableListOf()


    override val localVariables: List<IVariableDeclaration<IBlock>>
        get() {
            val vars = mutableListOf<IVariableDeclaration<IBlock>>()
            val v = object : IBlock.IVisitor {
                override fun visit(variable: IVariableDeclaration<IBlock>) {
                    vars.add(variable)
                }
            }
            block.accept(v)
            return vars
        }

    override val variables: List<IVariableDeclaration<*>>
        get() = parameters + localVariables

    override fun addParameter(type: IType): IParameter {
        val param: IParameter = VariableDeclaration(this, type)
        (parameters as MutableList).add(param)
        return param
    }

    override fun expression(args: List<IExpression>): IProcedureCallExpression {
        return ProcedureCall(NullBlock, this, -1, args.toMutableList())
    }

    override fun toString(): String = "static $returnType $id(" +
            parameters.joinToString(", ") {
                "${it.type.id} ${it.id ?: "$" + parameters.indexOf(it)}"
            } + ") $block"
}
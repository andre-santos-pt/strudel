package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.Block
import pt.iscte.strudel.model.impl.ProgramElement

/**
 * Mutable
 */
interface IProcedure : IProcedureDeclaration, IBlockHolder {
    val module: IModule?
    val localVariables: List<IVariableDeclaration<IBlock>>
    val variables: List<IVariableDeclaration<*>>
    override var returnType: IType

    val comment: String?

    val isRecursive: Boolean
        get() {
            class RecFind : IBlock.IVisitor {
                var foundRecursiveCall = false
                override fun visit(call: IProcedureCall): Boolean {
                    if (call.procedure == this@IProcedure) foundRecursiveCall = true
                    return true
                }

                override fun visit(exp: IProcedureCallExpression): Boolean {
                    if (exp.procedure == this@IProcedure) foundRecursiveCall = true
                    return true
                }
            }

            val r = RecFind()
            block.accept(r)
            return r.foundRecursiveCall
        }

    val isConstantTime: Boolean
        get() {
            if (isRecursive) return false
            class LoopAndCallFind : IBlock.IVisitor {
                var found = false
                override fun visit(loop: ILoop): Boolean {
                    found = true
                    return false
                }

                override fun visit(call: IProcedureCall): Boolean {
                    if (!(call.procedure as IProcedure).isConstantTime) found = true
                    return true
                }

                override fun visit(exp: IProcedureCallExpression): Boolean {
                    if (!(exp.procedure as IProcedure).isConstantTime) found = true
                    return true
                }
            }

            val v = LoopAndCallFind()
            block.accept(v)
            return !v.found
        }

    fun getVariable(id: String): IVariableDeclaration<*> = variables.find { it.id == id }!!

    fun accept(visitor: IBlock.IVisitor) {
        block.accept(visitor)
    }


}

internal class UnboundProcedure(id: String, override val comment: String? = null) : ProgramElement(), IProcedure {

    override val module: IModule? = null

    override val localVariables: List<IVariableDeclaration<IBlock>>
        get() = emptyList()

    override val variables: List<IVariableDeclaration<*>>
        get() = emptyList()

    init{
        setProperty(ID_PROP, id)
    }

    override val parameters: List<IVariableDeclaration<IProcedureDeclaration>> = emptyList()
    override var returnType: IType = UnboundType()

    override fun addParameter(type: IType): IVariableDeclaration<IProcedureDeclaration> {
        throw UnsupportedOperationException()
    }

    override fun expression(args: List<IExpression>): IProcedureCallExpression {
        throw UnsupportedOperationException()
    }

    override var block: IBlock = Block(this,false)
}
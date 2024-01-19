package pt.iscte.strudel.vm.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.impl.ProgramElement
import pt.iscte.strudel.model.impl.VariableDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

internal class ForeignProcedure(
    override val module: IModule?,
    override var namespace: String? = null,
    id: String,
    override val returnType: IType,
    parameterTypes: List<IType>,
    val action: (IVirtualMachine, List<IValue>) -> IValue?
) : ProgramElement(), IProcedureDeclaration {

    constructor(module: IModule? = null, namespace: String? = null, id: String, returnType: IType, param1: IType, action: (IVirtualMachine, List<IValue>) -> Unit)
            : this(module, namespace, id, returnType, listOf(param1), { vm:IVirtualMachine, args:List<IValue> -> action(vm, args); null })

    override var id: String? = id

    override val parameters: List<IParameter> = parameterTypes.map { VariableDeclaration(this, it) }

    override fun addParameter(type: IType): IParameter {
        throw UnsupportedOperationException()
    }

    override fun expression(args: List<IExpression>): IProcedureCallExpression {
        return ProcedureCall(NullBlock, this, -1, args.toMutableList())
    }

    fun run(vm: IVirtualMachine, args: List<IValue>) = action(vm, args)

    override val isForeign: Boolean
        get() = true
}

interface IForeignProcedure {
    companion object {
        fun create( namespace: String? = null,
                    id: String,
                    returnType: IType,
                    parameterTypes: List<IType>,
                    action: (IVirtualMachine, List<IValue>) -> IValue?): IProcedureDeclaration =
            ForeignProcedure(null, namespace, id, returnType, parameterTypes, action)
    }
}
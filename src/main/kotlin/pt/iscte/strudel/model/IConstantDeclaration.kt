package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.ConstantExpression


interface IConstantDeclaration : IModuleMember {
    val module: IModule
    var type: IType
    var value: IExpression

    fun expression() : IConstantExpression = ConstantExpression(this)
}
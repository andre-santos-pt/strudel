package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal class ConstantDeclaration(
    override val module: IModule,
    override var type: IType,
    override var value: IExpression) : ProgramElement(),
    IConstantDeclaration {

    override fun expression(): IConstantExpression {
        return ConstantExpression(this)
    }

    override fun toString(): String = "static final $type $id = $value;"
}

internal class ConstantExpression(override val constant: IConstantDeclaration)
    : Expression(), IConstantExpression, ITargetExpression
package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.ProgramElement

interface ISimpleExpression : IExpression

interface ILiteral : ISimpleExpression {
    val stringValue: String

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return false
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is ILiteral && stringValue == e.stringValue
    }

}



internal object NULL_LITERAL : ProgramElement(), ILiteral {
    override val stringValue: String  = "null"

    override fun toString(): String = "null"

    override val type: IType
        get() = VOID

    override fun length(): IArrayLength {
        TODO("Not yet implemented")
    }

    override fun element(index: IExpression): IArrayAccess {
        TODO("Not yet implemented")
    }

    override fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression {
        TODO("Not yet implemented")
    }

    override fun conditional(trueCase: IExpression, falseCase: IExpression): IConditionalExpression {
        TODO("Not yet implemented")
    }
}

interface IVariableExpression : ISimpleExpression, ITargetExpression {
    val variable: IVariableDeclaration<*>
    val isUnbound: Boolean
        get() = variable.isUnbound

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return this.variable === variable
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IVariableExpression && variable == e.variable
    }

    override var id: String?
        get() = variable.id
        set(id) {
            super<ISimpleExpression>.id = id
        }
}

interface IConstantExpression : ISimpleExpression {
    val constant: IConstantDeclaration
    override val type: IType
        get() = constant.type

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return false
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IConstantExpression && constant == e.constant
    }
}

interface IRecordAllocation : ISimpleExpression {
    val recordType: IRecordType
    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return false
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IRecordAllocation &&
                recordType.isSame(e.recordType)
    }
}

interface IRecordFieldExpression : ICompositeExpression, ITargetExpression {
    val target: ITargetExpression
    val field: IVariableDeclaration<IRecordType>
    override fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression
    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return target.includes(variable)
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IRecordFieldExpression &&
                target.isSame(e.target) && field == e.field
    }
}
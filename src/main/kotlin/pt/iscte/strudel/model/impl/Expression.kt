package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.*


internal abstract class Expression(vararg flags: String) : ProgramElement(*flags), IExpression {

    override fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression {
        check(this is ITargetExpression)
        return RecordFieldExpression(this as ITargetExpression, field)
    }

    override fun length(): IArrayLength {
        check(this is ITargetExpression)
        return ArrayLength(this as ITargetExpression)
    }

    override fun element(index: IExpression): IArrayAccess {
        check(this is ITargetExpression)
        return ArrayAccess(this as ITargetExpression, index)
    }

    override fun conditional(trueCase: IExpression, falseCase: IExpression): IConditionalExpression {
        return Conditional(this, trueCase, falseCase)
    }


    // TODO evaluate only one
    // ideia: decompose() -> iterador que para quando nao e preciso mais
    internal class Conditional(
        override val condition: IExpression,
        override val trueCase: IExpression,
        override val falseCase: IExpression) : Expression(),
        IConditionalExpression {
        override val parts: List<IExpression>
            get() = listOf(condition)

        override fun conditional(trueCase: IExpression, falseCase: IExpression): IConditionalExpression {
            return Conditional(this, trueCase, falseCase)
        }
    }
}
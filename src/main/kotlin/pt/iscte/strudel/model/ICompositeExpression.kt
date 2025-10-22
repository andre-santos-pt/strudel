package pt.iscte.strudel.model

interface ICompositeExpression : IExpression {
    val parts: List<IExpression>
}

interface IUnaryExpression : ICompositeExpression {
    val operator: IUnaryOperator
    val operand: IExpression
    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return operand.includes(variable)
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IUnaryExpression && operator == e.operator &&
                operand.isSame(e.operand)
    }
}

interface IUnaryOperator : IProgramElement {
    fun isValidFor(type: IType): Boolean
    fun getResultType(exp: IExpression): IType
    fun on(exp: IExpression): IUnaryExpression
}

interface IBinaryExpression : ICompositeExpression {
    val operator: IBinaryOperator
    var leftOperand: IExpression
    var rightOperand: IExpression

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return leftOperand.includes(variable) || rightOperand.includes(variable)
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IBinaryExpression && operator == e.operator &&
                leftOperand.isSame(e.leftOperand) &&
                rightOperand.isSame(e.rightOperand)
    }
}

interface IBinaryOperator : IProgramElement {
    fun isValidFor(left: IExpression, right: IExpression): Boolean
    fun getResultType(left: IExpression, right: IExpression): IType

    fun on(leftOperand: IExpression, rightOperand: IExpression): IBinaryExpression
}

interface IConditionalExpression : ICompositeExpression {
    val condition: IExpression
    val trueCase: IExpression
    val falseCase: IExpression

    override val type: IType
        get() = trueCase.type

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return condition.includes(variable) ||
                trueCase.includes(variable) ||
                falseCase.includes(variable)
    }

    override fun isSame(e: IProgramElement): Boolean {
        if (e is IConditionalExpression) {
            return condition.isSame(e.condition) &&
                    trueCase.isSame(e.trueCase) &&
                    falseCase.isSame(e.falseCase)
        }
        return false
    }
}

interface IArrayAllocation : ICompositeExpression {
    val componentType: IType
    val dimensions: List<IExpression?>

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        for (e in dimensions) if (e?.includes(variable) == true) return true
        return false
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IArrayAllocation &&
                type.isSame(e.type) &&
                dimensions.size == e.dimensions.size &&
                dimensions.zip(e.dimensions).all { (d1, d2) ->
                    if (d1 == null && d2 == null) true
                    else if (d1 != null && d2 != null) d1.isSame(d2)
                    else false
                }
               // IExpression.areSame(dimensions, e.dimensions)
    }
}

interface IPredefinedArrayAllocation : IArrayAllocation {
    val elements: List<IExpression>

    override val type: IType
        get() = componentType.array().reference()


    override val dimensions: List<IExpression>
        get() = listOf(INT.literal(elements.size))
}

interface IArrayAccess : ITargetExpression, ICompositeExpression {
    val target: ITargetExpression

    val index: IExpression

    override val type: IType
        get() = if(target.type.isArrayReference)
            ((target.type as IReferenceType).target as IArrayType).componentType
        else if (target.type.isArray)
            target.type.asArrayType.componentType // TODO review target type
        else
            target.type

    override val parts get() = listOf(target, index)

    override fun isSame(e: IProgramElement): Boolean {
        return e is IArrayAccess &&
                target.isSame(e.target) && index.isSame(e.index)
    }

    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        if (target.includes(variable)) return true
        if(index.includes(variable)) return true
        return false
    }
}

interface IArrayLength : ICompositeExpression {
    val target: ITargetExpression
    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        return target.includes(variable)
    }

    override fun isSame(e: IProgramElement): Boolean {
        return e is IArrayLength &&
                target.isSame(e.target)
    }
}

interface IProcedureCallExpression : IProcedureCall, ICompositeExpression, ITargetExpression {
    override fun includes(variable: IVariableDeclaration<*>): Boolean {
        for (arg in arguments)
            if (arg.includes(variable)) return true
        return false
    }
}

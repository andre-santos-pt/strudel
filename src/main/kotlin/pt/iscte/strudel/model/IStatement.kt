package pt.iscte.strudel.model

/**
 * Immutable
 */
interface IStatement : IBlockElement {
    val expressionParts: List<IExpression>

    override fun isSame(s: IProgramElement): Boolean = false
}

interface IInnocuousStatement : IStatement



interface IBreak : IStatement {
    override val parent: IBlock
    override val expressionParts: List<IExpression>
        get() = emptyList()

    override fun isSame(s: IProgramElement) = s is IBreak
}


interface IContinue : IStatement {
    override val parent: IBlock
    override val expressionParts: List<IExpression>
        get() = emptyList()

    override fun isSame(s: IProgramElement) = s is IContinue
}

interface IReturn : IStatement {
    var expression: IExpression?
    override val parent: IBlock
    val isError: Boolean
    val errorMessage: String?
    
    val isVoid: Boolean
        get() = expression == null

    val returnValueType: IType
        get() = if (isVoid) VOID else expression!!.type

    override val expressionParts: List<IExpression>
        get() = if (expression == null) emptyList() else listOf(expression as IExpression)

    override fun isSame(s: IProgramElement): Boolean {
        return s is IReturn &&
                (isVoid && s.isVoid || expression!!.isSame(s.expression!!))
    }
}

interface IVariableAssignment : IStatement {
    // OCL: variable must be owned by the same procedure
    var target: IVariableDeclaration<*>
    var expression: IExpression
    override val parent: IBlock

    override val expressionParts: List<IExpression>
        get() = listOf(expression)

    override fun isSame(s: IProgramElement): Boolean {
        return s is IVariableAssignment && target == s.target &&
                expression.isSame(s.expression)
    }

    companion object {
        private fun isModifiedByOne(ass: IVariableAssignment, op: IBinaryOperator): Boolean {
            if (ass.expression !is IBinaryExpression) return false
            val ONE = INT.literal(1)
            val exp = ass.expression as IBinaryExpression
            return exp.operator.isSame(op) &&
                    exp.leftOperand.isSame(ass.target) && exp.rightOperand.isSame(ONE) ||
                    exp.rightOperand.isSame(ass.target) && exp.leftOperand.isSame(ONE)
        }
    }
}

interface IArrayElementAssignment : IStatement {
    val arrayAccess: IArrayAccess
    val expression: IExpression
    override val parent: IBlock
    val dimensions: Int
        get() = 1

    override val expressionParts: List<IExpression>
        get() = listOf(arrayAccess.index)

    override fun isSame(s: IProgramElement): Boolean {
        return s is IArrayElementAssignment &&
                arrayAccess.isSame(s.arrayAccess) &&
                IExpression.areSame(expressionParts, (s as IStatement).expressionParts)
    }
}


interface IRecordFieldAssignment : IStatement {
    val target: ITargetExpression
    val field: IVariableDeclaration<IRecordType>
    val expression: IExpression
    override val expressionParts: List<IExpression>
        get() = listOf(expression)
}

interface IProcedureCall : IStatement {
    val procedure: IProcedureDeclaration
    val arguments: List<IExpression>
    val isOperation: Boolean
        get() = false
    val isBound: Boolean
        get() = procedure !is UnboundProcedure

    override fun isSame(e: IProgramElement): Boolean {
        return e is IProcedureCall && procedure == e.procedure &&
                IExpression.areSame(arguments, e.arguments)
    }
}
package pt.iscte.strudel.model


// TODO expression copy on statement copy
/**
 * Immutable
 *
 */
interface IExpression : IProgramElement {
    val type: IType




    fun length(): IArrayLength
//    fun length(vararg indexes: IExpression): IArrayLength {
//        return length(listOf(*indexes))
//    }

    fun element(index: IExpression): IArrayAccess
//    fun element(vararg indexes: IExpression): IArrayElement {
//        return element(listOf(*indexes))
//    }

    //operator fun get(index: IExpression) = element(*indexes)

    fun field(field: IVariableDeclaration<IRecordType>): IRecordFieldExpression
    val isSimple: Boolean
        get() = this is ISimpleExpression
    val isComposite: Boolean
        get() = this is ICompositeExpression


    val isNull: Boolean
        get() = this === NULL_LITERAL

    fun conditional(trueCase: IExpression, falseCase: IExpression): IConditionalExpression


    fun includes(variable: IVariableDeclaration<*>): Boolean
    fun accept(visitor: IVisitor) {
        visitPart(visitor, this)
    }

    interface IVisitor {
        fun visit(exp: IArrayAllocation) = true
        fun visit(exp: IArrayLength): Boolean {
            return true
        }

        fun visit(exp: IArrayAccess): Boolean {
            return true
        }

        fun visit(exp: IUnaryExpression): Boolean {
            return true
        }

        fun visit(exp: IBinaryExpression): Boolean {
            return true
        }

        fun visit(exp: IProcedureCallExpression): Boolean {
            return true
        }

        fun visit(exp: IConditionalExpression): Boolean {
            return true
        }

        fun visit(exp: ILiteral) {}
        fun visit(exp: IVariableExpression) {}
        fun visit(exp: IConstantExpression) {}
        fun visit(exp: IRecordAllocation) {}
        fun visit(exp: IRecordFieldExpression) {}
        fun visitAny(exp: IExpression) {}
    }


    companion object {
        fun areSame(a: List<IExpression>, b: List<IExpression>): Boolean {
            if (a.size != b.size) return false
            var i = 0
            for (e in a) if (!e.isSame(b[i++])) return false
            return true
        }

        fun visitPart(visitor: IVisitor, part: IExpression) {
            visitor.visitAny(part)
            if (part is IArrayAllocation) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IArrayLength) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IArrayAccess) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IUnaryExpression) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IBinaryExpression) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IProcedureCallExpression) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is IConditionalExpression) {
                if (visitor.visit(part)) part.parts.forEach { p: IExpression -> visitPart(visitor, p) }
            } else if (part is ILiteral) {
                visitor.visit(part)
            } else if (part is IRecordAllocation) {
                visitor.visit(part)
            } else if (part is IRecordFieldExpression) {
                visitor.visit(part)
            } else if (part is IVariableExpression) {
                visitor.visit(part)
            } else if (part is IConstantExpression) {
                visitor.visit(part)
            } else check(false) { "missing case $part" }
        }
    }
}

interface ITargetExpression : IExpression
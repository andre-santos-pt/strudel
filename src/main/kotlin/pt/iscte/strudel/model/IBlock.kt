package pt.iscte.strudel.model

import pt.iscte.strudel.model.impl.*
import pt.iscte.strudel.model.impl.Break
import pt.iscte.strudel.model.impl.Continue
import pt.iscte.strudel.model.impl.Loop
import pt.iscte.strudel.model.impl.Return
import pt.iscte.strudel.model.impl.VariableDeclaration

interface IBlockHolder : IProgramElement {
    var block: IBlock

    fun addVariable(type: IType, vararg flags: String): IVariableDeclaration<IBlock> =
        VariableDeclaration(block, type, flags = *flags)

    fun addAssignment(
        target: IVariableDeclaration<IBlock>,
        exp: IExpression,
        vararg flags: String
    ): IVariableAssignment = VariableAssignment(block, target, exp, flags = *flags)

//    fun addRecordFieldAssignment(field: IRecordFieldExpression, exp: IExpression): IRecordFieldAssignment =
//        RecordFieldAssignment(block, field, exp)

    fun addArrayElementAssignment(
        target: ITargetExpression,
        expression: IExpression,
        index: IExpression
    ): IArrayElementAssignment = ArrayElementAssignment(block, target, expression, arrayIndex = index)

    fun addReturn(): IReturn = Return(block)

    //@JsName("addReturnExp")
    fun addReturn(exp: IExpression): IReturn = Return(block, exp)
    fun addReturnError(exp: IExpression): IReturn = Return(block, exp, isError = true)

    fun addSelection(guard: IExpression, vararg flags: String): ISelection =
        Selection(block, guard, hasAlternative = false)

    fun addSelectionWithAlternative(guard: IExpression, vararg flags: String): ISelection =
        Selection(block, guard, hasAlternative = true)

    fun addLoop(guard: IExpression, vararg flags: String): ILoop = Loop(block, guard, flags = *flags)
    fun addBreak(): IBreak = Break(block)
    fun addContinue(): IContinue = Continue(block)

    fun addCall(p: IProcedureDeclaration, arguments: List<IExpression>): IProcedureCall =
        ProcedureCall(this, p, arguments = arguments.toMutableList())

    fun addBlock(): IBlock = Block(this, true)
}

object NullBlock : IBlockHolder {
    override var block: IBlock
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun setProperty(key: String, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun getProperty(key: String): Any? {
        TODO("Not yet implemented")
    }

    override fun cloneProperties(e: IProgramElement) {
        TODO("Not yet implemented")
    }
}

interface IBlockElement : IProgramElement {
    val parent: IBlockHolder

    val index get() = parent.block.children.indexOf(this)

    val ownerProcedure: IProcedure
        get() {
            var p = parent
            while (p !is IProcedure) p = (p as IBlockElement).parent
            return p
        }

    fun remove() {
        val parent = parent
        if (parent is IBlock)
            parent.remove(this)
        else if (parent is IRecordType)
            parent.removeField(this as IVariableDeclaration<IRecordType>)
    }

    fun copyTo(newParent: IBlockHolder, index: Int = -1) : IBlockElement
}

interface IBlock : IBlockElement, Iterable<IBlockElement>, IBlockHolder {
    val procedure: IProcedure?
    val children: List<IBlockElement>
    val size: Int
    open val isEmpty: Boolean
        get() = size == 0

    val isStatementBlock get() = parent is IBlock

    override var block: IBlock
        get() = this
        set(value) {
            throw UnsupportedOperationException()
        }

    val first: IBlockElement?
        get() {
            check(!isEmpty)
            return children[0]
        }

    val last: IBlockElement?
        get() {
            check(!isEmpty)
            return children[size - 1]
        }

    val depth: Int
        get() =
            if (parent is IProcedure) 1
            else if (parent is IControlStructure) 1 + (parent as IControlStructure).parent.depth
            else if (parent is IBlock) 1 + (parent as IBlock).depth
            else 0

    fun getPrevious(e: IBlockElement): IBlockElement? {
        require(children.contains(e))
        val i = children.indexOf(e)
        return if (i == 0) null else children[i - 1]
    }

    fun getNext(e: IBlockElement): IBlockElement? {
        require(children.contains(e))
        val i = children.indexOf(e)
        return if (i == children.size - 1) null else children[i + 1]
    }


    fun add(element: IBlockElement, index: Int = -1)

    fun remove(element: IBlockElement)

    fun moveTo(element: IBlockElement, index: Int)

    fun moveAfter(element: IBlockElement, target: IBlockElement) {
        require(element in block)
        require(target in block)
        moveTo(element, block.indexOf(target))
    }


//    operator fun contains(type: Class<out IBlockElement>): Boolean {
//        for (e in children) if (type.isInstance(e)) return true
//        return false
//    }

    override fun iterator(): Iterator<IBlockElement> {
        return children.iterator()
    }

    fun deepIterator(): Iterator<IBlockElement> {
        val list: MutableList<IBlockElement> = mutableListOf()
        accept(object : IVisitor {
            override fun visitAny(element: IBlockElement) {
                list.add(element)
            }
        })
        return list.iterator()
    }

    val isInLoop: Boolean
        get() {
            var p = parent
            while (p !is IProcedure)
                p = if (p is ILoop) return true else (p as IBlockElement).parent
            return false
        }

    fun filter(predicate: (IBlockElement) -> Boolean): List<IBlockElement> {
        val list: MutableList<IBlockElement> = mutableListOf()
        accept(object : IVisitor {
            override fun visitAny(element: IBlockElement) {
                list.add(element)
            }
        })
        return list.filter(predicate)
    }

    fun accept(visitor: IVisitor) {
        children.forEach {
            visitor.visitAny(it)
            if (it is IReturn) {
                if (visitor.visit(it) && !it.returnValueType.isVoid) it.expression!!.accept(visitor)
            } else if (it is IVariableDeclaration<*>) {
                visitor.visit(it as IVariableDeclaration<IBlock>)
            } else if (it is IArrayElementAssignment) {
                if (visitor.visit(it)) {
                    it.arrayAccess.index.accept(visitor)
                    //it.arrayAccess.indexes.forEach { i: IExpression -> i.accept(visitor) }
                    it.expression.accept(visitor)
                }
            } else if (it is IRecordFieldAssignment) {
                if (visitor.visit(it)) it.expression.accept(visitor)
            } else if (it is IVariableAssignment) {
                if (visitor.visit(it)) it.expression.accept(visitor)
            } else if (it is IProcedureCall) {
                if (visitor.visit(it)) it.arguments.forEach { a: IExpression -> a.accept(visitor) }
            } else if (it is IBreak) {
                visitor.visit(it)
            } else if (it is IContinue) {
                visitor.visit(it)
            } else if (it is ISelection) {
                if (visitor.visit(it)) {
                    it.guard.accept(visitor)
                    it.block.accept(visitor)
                    visitor.endVisitBranch(it)
                    if (it.hasAlternativeBlock()) {
                        visitor.visitAlternative(it)
                        it.alternativeBlock!!.accept(visitor)
                        visitor.endVisitAlternative(it)
                    }
                }
                visitor.endVisit(it)
            } else if (it is ILoop) {
                if (visitor.visit(it)) {
                    it.guard.accept(visitor)
                    it.block.accept(visitor)
                }
                visitor.endVisit(it)
            } else if (it is IBlock) { // only single blocks
                if (visitor.visit(it)) it.accept(visitor)
                visitor.endVisit(it)
            } else check(false) { "missing case $it" }
        }
    }

    interface IVisitor : IExpression.IVisitor {
        // IStatement
        fun visit(returnStatement: IReturn): Boolean {
            return true
        }

        fun visit(assignment: IArrayElementAssignment): Boolean {
            return true
        }

        fun visit(assignment: IVariableAssignment): Boolean {
            return true
        }

        fun visit(assignment: IRecordFieldAssignment): Boolean {
            return true
        }

        fun visit(call: IProcedureCall): Boolean {
            return true
        }

        // IControlStructure
        fun visit(selection: ISelection): Boolean {
            return true
        }

        fun endVisit(selection: ISelection) {}
        fun visitAlternative(selection: ISelection): Boolean {
            return true
        }

        fun endVisitBranch(selection: ISelection) {}
        fun endVisitAlternative(selection: ISelection) {}
        fun visit(loop: ILoop): Boolean {
            return true
        }

        fun endVisit(loop: ILoop) {}

        // other
        fun visit(block: IBlock): Boolean {
            return true
        }

        fun endVisit(block: IBlock) {}
        fun visit(breakStatement: IBreak) {}
        fun visit(continueStatement: IContinue) {}
        fun visit(variable: IVariableDeclaration<IBlock>) {}
        fun visitAny(element: IBlockElement) {}
    }
}



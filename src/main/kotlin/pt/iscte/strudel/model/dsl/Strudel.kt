package pt.iscte.strudel.model.dsl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.*
import pt.iscte.strudel.model.impl.Loop
import pt.iscte.strudel.model.impl.RecordType
import pt.iscte.strudel.model.impl.Return
import pt.iscte.strudel.model.impl.Selection
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.model.util.findAll

@JvmOverloads
fun module(id: String? = null, configure: IModule.() -> Unit = {}): IModule {
    val m = Module()
    id?.let {
        m.id = id
    }
    configure(m)
    return m
}

@JvmOverloads
fun IModule.Record(id: String? = null, configure: IRecordType.() -> Unit = {}): IRecordType {
    val t = RecordType(this)
    id?.let {
        t.id = it
        t.namespace = it
    }
    configure(t)
    return t
}

@JvmOverloads
fun IRecordType.Field(type: IType, id: String? = null): IField {
    val f = this.addField(type)
    id?.let { f.id = id }
    return f
}

@JvmOverloads
fun IModule.Procedure(returnType: IType, id: String? = null, configure: IBlock.() -> Unit = {}
): IProcedure {
    val p = Procedure(this, returnType)
    id?.let { p.id = id }
    configure(p.block)
    return p
}

fun Procedure(returnType: IType, id: String? = null, configure: IBlock.() -> Unit = {}
): IProcedure {
    val p = Procedure(Module(), returnType)
    id?.let { p.id = id }
    configure(p.block)
    return p
}

val IProcedure.loops get() = findAll(ILoop::class)
fun IProcedure.loop(index: Int) = find(ILoop::class, index)

@JvmOverloads
fun IModule.Procedure(procedure: IProcedureDeclaration): IProcedureDeclaration {
    members.add(procedure)
    return procedure
}

@JvmOverloads
fun IBlock.Param(type: IType, id: String? = null): IVariableDeclaration<IProcedureDeclaration> {
    val p = (parent as IProcedure).addParameter(type)
    id?.let { p.id = id }
    return p
}


@JvmOverloads
fun IModule.Constant(type: IType, id: String? = null, expression: IExpression)
: IConstantDeclaration {
    val c = ConstantDeclaration(this, type, expression)
    id?.let { c.id = id }
    return c
}



@JvmOverloads
fun IBlock.If(guard: IExpression, content: IBlock.() -> Unit = {}): ISelection {
    val s = Selection(this, guard)
    content(s.block)
    return s
}

fun ISelection.Else(content: IBlock.() -> Unit = {}) {
    createAlternativeBlock().content()
}

fun IBlock.While(guard: IExpression, content: IBlock.() -> Unit = {}): ILoop {
    val l = Loop(this, guard)
    content(l.block)
    return l
}

fun IBlock.Block(vararg flags: String, content: IBlock.() -> Unit = {}): IBlock {
    val b = this.addBlock()
    content(b)
    flags.forEach {
        b.setFlag(it)
    }
    return b
}

fun IBlock.Call(procedure: IProcedureDeclaration, vararg arguments: IExpression): IProcedureCall {
    return ProcedureCall(this, procedure, arguments = arguments.toMutableList())
}

fun callExpression(procedure: IProcedureDeclaration, vararg arguments: IExpression): IProcedureCallExpression {
    return ProcedureCall(NullBlock, procedure, arguments = arguments.toMutableList())
}

@JvmOverloads
fun IBlock.Return(v: IVariableDeclaration<*>): IReturn = Return(this, VariableExpression(v))

fun IBlock.Return(e: IExpression): IReturn = Return(this, e)

fun IBlock.ReturnVoid(): IReturn = Return(this, null)

fun IBlock.ReturnError(msg: String): IReturn = Return(this, null, isError = true, errorMessage = msg)


fun IBlock.Break(): IBreak = Break(this)

fun IBlock.Continue(): IContinue = Continue(this)

fun IBlock.Var(type: IType, id: String? = null, init: IExpression? = null): IVariableDeclaration<IBlock> {
    var dec = VariableDeclaration(this, type)
    id?.let {dec.id = id}
    init?.let {
        Assign(dec, init)
    }
    return dec
}

fun IBlock.Var(type: IType, id: String? = null, init: Number): IVariableDeclaration<IBlock> = Var(type, id, lit(init))

fun IBlock.Var(type: IType, init: Number): IVariableDeclaration<IBlock> = Var(type, null, lit(init))

fun IBlock.Var(type: IType, init: IExpression): IVariableDeclaration<IBlock> = Var(type, null, init)

fun IVariableDeclaration<*>.exp(): IVariableExpression = VariableExpression(this)

fun IBlock.Assign(v: IVariableDeclaration<*>, e: IExpression): IVariableAssignment = VariableAssignment(this, v, e)

fun IBlock.Assign(v: IVariableDeclaration<*>, n: Number): IVariableAssignment = VariableAssignment(this, v, lit(n))

fun IBlock.ArraySet(v: IVariableDeclaration<*>, index: IExpression, e: IExpression): IArrayElementAssignment =
    ArraySet(v.expression(), index, e)

fun IBlock.ArraySet(v: ITargetExpression, index: IExpression, e: IExpression): IArrayElementAssignment =
    ArrayElementAssignment(this, v, e, arrayIndex = index)

fun IBlock.FieldSet(variable: IVariableDeclaration<*>, f: IVariableDeclaration<IRecordType>, e: IExpression, index: Int = -1) : IRecordFieldAssignment =
    FieldSet(variable.expression(), f, e, index)

fun IBlock.FieldSet(s: ITargetExpression, f: IVariableDeclaration<IRecordType>, e: IExpression, index: Int = -1) : IRecordFieldAssignment =
    RecordFieldAssignment(this, s, f, e, index)

fun lit(n: Number) =
    if (n is Int) INT.literal(n)
    else DOUBLE.literal(n as Double)

fun character(c: Char) =
    CHAR.literal(c)

fun character(code: Int) =
    CHAR.literal(code.toChar())

val True: ILiteral = BOOLEAN.literal(true)

val False: ILiteral = BOOLEAN.literal(false)


fun array(type: IType): IType {
    return type.array().reference()
}

fun exp(e: IVariableDeclaration<*>) = e.expression()

fun IVariableDeclaration<*>.length() : IExpression {
    return expression().length()
}

fun Length(v: IVariableDeclaration<*>) : IExpression {
    return v.expression().length()
}

operator fun IVariableDeclaration<*>.get(i : IExpression): IArrayAccess = expression().element(i)

operator fun IVariableDeclaration<*>.get(i : IVariableDeclaration<*>) = expression().element(
    exp(i)
)

operator fun IArrayAccess.get(i : IExpression) = element(i)


infix operator fun IVariableDeclaration<*>.plus(rightOperand: IExpression): IBinaryExpression {
    return expression().plus(rightOperand)
}

infix operator fun IVariableDeclaration<*>.plus(rightOperand: Number): IBinaryExpression {
    return expression().plus(rightOperand)
}

infix operator fun IExpression.plus(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.ADD.on(this, rightOperand)
}

infix operator fun Number.plus(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.ADD.on(if(this is Int) INT.literal(this.toInt()) else DOUBLE.literal(this.toDouble()), rightOperand)
}

infix operator fun IExpression.plus(rightOperand: Number): IBinaryExpression {
    return ArithmeticOperator.ADD.on(this, if(rightOperand is Int) INT.literal(rightOperand.toInt()) else DOUBLE.literal(rightOperand.toDouble()))
}



infix operator fun IVariableDeclaration<*>.minus(rightOperand: IExpression): IBinaryExpression {
    return expression().minus(rightOperand)
}

infix operator fun IVariableDeclaration<*>.minus(rightOperand: Number): IBinaryExpression {
    return expression().minus(lit(rightOperand))
}


infix operator fun IVariableDeclaration<*>.minus(rightOperand: IVariableDeclaration<*>): IBinaryExpression {
    return expression().minus(exp(rightOperand))
}

infix operator fun IExpression.minus(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.SUB.on(this, rightOperand)
}

infix operator fun Number.minus(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.SUB.on(if(this is Int) INT.literal(this.toInt()) else DOUBLE.literal(this.toDouble()), rightOperand)
}

infix operator fun IExpression.minus(rightOperand: Number): IBinaryExpression {
    return ArithmeticOperator.SUB.on(this, if(rightOperand is Int) INT.literal(rightOperand.toInt()) else DOUBLE.literal(rightOperand.toDouble()))
}


infix operator fun IExpression.times(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.MUL.on(this, rightOperand)
}

infix operator fun Number.times(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.MUL.on(if(this is Int) INT.literal(this.toInt()) else DOUBLE.literal(this.toDouble()), rightOperand)
}

infix operator fun IExpression.times(rightOperand: Number): IBinaryExpression {
    return ArithmeticOperator.MUL.on(this, if(rightOperand is Int) INT.literal(rightOperand.toInt()) else DOUBLE.literal(rightOperand.toDouble()))
}


infix operator fun IExpression.div(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.DIV.on(this, rightOperand)
}

infix operator fun Number.div(rightOperand: IExpression): IBinaryExpression {
    return ArithmeticOperator.DIV.on(if(this is Int) INT.literal(this.toInt()) else DOUBLE.literal(this.toDouble()), rightOperand)
}

infix operator fun IExpression.div(rightOperand: Number): IBinaryExpression {
    return ArithmeticOperator.DIV.on(this, if(rightOperand is Int) INT.literal(rightOperand.toInt()) else DOUBLE.literal(rightOperand.toDouble()))
}


infix fun IExpression.equal(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.EQUAL.on(this, rightOperand)
}

infix fun IExpression.equal(rightOperand: IVariableDeclaration<*>): IBinaryExpression {
    return RelationalOperator.EQUAL.on(this, exp(rightOperand))
}

infix fun IExpression.notEqual(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.DIFFERENT.on(this, rightOperand)
}

infix fun IExpression.smaller(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.SMALLER.on(this, rightOperand)
}

infix fun IExpression.smaller(rightOperand: IVariableDeclaration<*>): IBinaryExpression {
    return RelationalOperator.SMALLER.on(this, exp(rightOperand))
}

infix fun IExpression.smallerEq(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.SMALLER_EQUAL.on(this, rightOperand)
}

infix fun IVariableDeclaration<*>.smallerEq(rightOperand: IVariableDeclaration<*>): IBinaryExpression {
    return RelationalOperator.SMALLER_EQUAL.on(this.expression(), rightOperand.expression())
}

infix fun IExpression.greater(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.GREATER.on(this, rightOperand)
}

infix fun IExpression.greaterEq(rightOperand: IExpression): IBinaryExpression {
    return RelationalOperator.GREATER_EQUAL.on(this, rightOperand)
}


package pt.iscte.strudel.javaparser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.UnaryOperator
import java.io.File


const val JP = "JP"
const val FOR = "FOR"
const val EFOR = "EFOR"

const val ID_LOC = "ID_LOC"
const val TYPE_LOC = "TYPE_LOC"
const val OPERATOR_LOC = "OPERATOR_LOC"

val defaultTypes = mapOf(
    "void" to VOID,

    //"Object" to ANY,

    "int" to INT,
    "double" to DOUBLE,
    "char" to CHAR,
    "boolean" to BOOLEAN,

    "int[]" to INT.array().reference(),
    "double[]" to DOUBLE.array().reference(),
    "char[]" to CHAR.array().reference(),
    "boolean[]" to BOOLEAN.array().reference(),

    "int[][]" to INT.array().reference().array().reference(),
    "double[][]" to DOUBLE.array().reference().array().reference(),
    "char[][]" to CHAR.array().reference().array().reference(),
    "boolean[][]" to BOOLEAN.array().reference().array().reference(),

    String::class.java.name to JavaType(String::class.java)
)

fun MutableMap<String, IType>.mapType(t: String): IType =
    if (containsKey(t))
        this[t]!!
    else {
        try {
            JavaType(Class.forName(t))
        } catch (e1: Exception) {
            try {
                JavaType(Class.forName("java.lang.$t"))
            } catch (e2: Exception) {
                try {
                    JavaType(Class.forName("java.util.$t"))
                } catch (e3: Exception) {
                    unsupported("type", t)
                }
            }
        }
    }

fun MutableMap<String, IType>.mapType(t: Type) = mapType(t.asString())

fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) = mapType(t.nameAsString)

// inline because otherwise test fails (KotlinNothingValueException)
inline fun unsupported(msg: String, node: Any): Nothing {
    throw AssertionError("unsupported $msg: $node (${node::class.java})")
}

inline fun error(msg: String, node: Any): Nothing {
    throw AssertionError("compile error $msg: $node (${node::class.java})")
}

fun typeSolver(): TypeSolver {
    val combinedTypeSolver = CombinedTypeSolver()
//        combinedTypeSolver.add(JreTypeSolver())
//        combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
//        combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return combinedTypeSolver
}

class Java2Strudel(
    val foreignTypes: List<IType> = emptyList(),
    val foreignProcedures: List<IProcedureDeclaration> = emptyList(),
    val bindSource: Boolean = true,
    val bindJavaParser: Boolean = true
) {

    val jpFacade = JavaParserFacade.get(typeSolver())

    private fun <T : IProgramElement> T.bind(node: Node, prop: String? = null): T {
        if (bindSource && node.range.isPresent) {
            val range = node.range.get()
            val sourceLocation = SourceLocation(range.begin.line, range.begin.column, range.end.column)
            if (prop == null)
                setProperty(sourceLocation)
            else
                setProperty(prop, sourceLocation)
        }
        if (bindJavaParser)
            setProperty(JP, node)
        return this
    }

    fun load(file: File): IModule =
        translate(StaticJavaParser.parse(file).types.filterIsInstance<ClassOrInterfaceDeclaration>())

    fun load(files: List<File>): IModule =
        translate(files.map { StaticJavaParser.parse(it).types.filterIsInstance<ClassOrInterfaceDeclaration>() }
            .flatten())

    fun load(src: String): IModule =
        translate(StaticJavaParser.parse(src).types.filterIsInstance<ClassOrInterfaceDeclaration>())

    fun collectHostTypes(types: MutableMap<String, IType>, type: ClassOrInterfaceDeclaration): List<HostRecordType> {
        val list = mutableListOf<HostRecordType>()
        type.accept(object : VoidVisitorAdapter<Any>() {
            override fun visit(n: VariableDeclarator, arg: Any?) {
                var t = n.type
                while(t.isArrayType)
                    t = t.asArrayType().componentType

                val typeName = t.asString()

                if (typeName !in types) {
                    try {
                        Class.forName(typeName)
                        list.add(HostRecordType(typeName))
                    } catch (e: Exception) {
                        val langName = "java.lang.$typeName"
                        try {
                            Class.forName(langName)
                            list.add(HostRecordType(langName))
                        } catch (e: Exception) {
                            try {
                                val typeNameUtil = "java.util.$typeName"
                                Class.forName(typeNameUtil)
                                list.add(HostRecordType(typeNameUtil))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }, null)
        return list
    }

    fun translate(classes: List<ClassOrInterfaceDeclaration>) = module {
        val types = defaultTypes.toMutableMap()

        classes.forEach { c ->
            if (c.extendedTypes.isNotEmpty())
                unsupported("extends", c)

            if (c.implementedTypes.isNotEmpty())
                unsupported("implements", c)

            val type = Record(c.nameAsString) {
                bind(c.name, ID_LOC)
            }
            types[c.nameAsString] = type.reference()
            types[c.nameAsString + "[]"] = type.array().reference()
            types[c.nameAsString + "[][]"] = type.array().array().reference()
        }

        classes.forEach { c ->
            collectHostTypes(types, c).forEach {
                types[it.id] = it
                types[it.id+ "[]"] = it
                types[it.id+ "[][]"] = it
            }
        }

        classes.forEach { c ->
            val recordType = (types[c.nameAsString] as IReferenceType).target as IRecordType
            c.fields.forEach { f ->
                recordType.addField(types[f.variables[0].typeAsString]!!) {
                    id = f.variables[0].nameAsString
                }
            }
        }


        fun MethodDeclaration.translateMethod(namespace: String) =
            Procedure(types.mapType(type), nameAsString).apply {
                if (!modifiers.contains(Modifier.staticModifier()))
                    addParameter(types.mapType((parentNode.get() as ClassOrInterfaceDeclaration))).apply {
                        id = "this"
                    }
                this@translateMethod.parameters.forEach { p ->
                    addParameter(types.mapType(p.type)).apply {
                        id = p.nameAsString
                    }.bind(p)
                        .bind(p.type, TYPE_LOC)
                        .bind(p.name, ID_LOC)
                }
                bind(this@translateMethod)
                setProperty(NAMESPACE_PROP, namespace)
                bind(name, ID_LOC)
                bind(this@translateMethod.type, TYPE_LOC)
            }

        val procedures = classes.map { it.methods }.flatten().map {
            it to it.translateMethod((it.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString)
        }

        procedures.forEach {
            it.first.body.get().translate(it.second, it.second.block, procedures, types)
        }
    }


    fun Statement.translate(
        procedure: IProcedure,
        block: IBlock,
        procedures: List<Pair<MethodDeclaration, IProcedure>>,
        types: MutableMap<String, IType>
    ) {

        fun findProcedure(id: String, paramTypes: List<ResolvedType>): IProcedureDeclaration? {
            println(paramTypes)
            val find = procedures.find { it.first.nameAsString == id }
            if (find != null)
                return find.second

            val findForeign = foreignProcedures.find { it.id == id }
            return findForeign
        }

        if (this is EmptyStmt)
            return
        else if (this is BlockStmt) {
            this.statements.forEach {
                it.translate(procedure, block, procedures, types)
            }
            return
        }

        fun getType(type: Type) =
            if (type.isArrayType) types[type.elementType.asString()]!!.array()
            else types[type.asString()]

        fun mapExpression(exp: Expression): IExpression =
            when (exp) {
                is IntegerLiteralExpr -> lit(exp.value.toInt())
                is DoubleLiteralExpr -> lit(exp.value.toDouble())
                is CharLiteralExpr -> character(exp.value[0])
                is BooleanLiteralExpr -> if (exp.value) True else False

                is NameExpr -> procedure.variables.find { it.id == exp.nameAsString }?.expression()
                    ?: error("not found", exp)//UnboundVariableDeclaration(exp.nameAsString, procedure).expression()

                is NullLiteralExpr -> NULL_LITERAL

                // TODO String literal?
//                is StringLiteralExpr -> {
//
//                }

                is UnaryExpr -> mapUnOperator(exp).on(mapExpression(exp.expression)).apply {
                    // TODO review
                    val from = exp.range.get().begin.column
                    val to = exp.expression.range.get().begin.column - 1
                    setProperty(OPERATOR_LOC, SourceLocation(exp.expression.range.get().begin.line, from, to))
                }

                is BinaryExpr -> mapBiOperator(exp).on(mapExpression(exp.left), mapExpression(exp.right)).apply {
                    if (exp.left.range.isPresent && exp.right.range.isPresent && exp.left.range.get().begin.line == exp.right.range.get().begin.line) {
                        val from = exp.left.range.get().end.column + 1
                        val to = exp.right.range.get().begin.column - 1
                        setProperty(OPERATOR_LOC, SourceLocation(exp.left.range.get().begin.line, from, to))
                    }
                }

                is EnclosedExpr -> mapExpression(exp.inner)

                is CastExpr -> UnaryOperator.TRUNCATE.on(mapExpression(exp.expression)) //TODO other casts

                is ArrayCreationExpr -> types.mapType(exp.elementType.asString()).array()
                    .heapAllocation(exp.levels.map { mapExpression(it.dimension.get()) }) // TODO multi level

                is ArrayAccessExpr -> mapExpression(exp.name).element(mapExpression(exp.index))

                is ObjectCreationExpr -> types.mapType(exp.type).asRecordType.heapAllocation()

                is FieldAccessExpr -> {
                    if (exp.scope is ArrayAccessExpr && exp.nameAsString == "length") {
                        mapExpression(exp.scope).length()
                    }
                    else {
                        val solve = jpFacade.solve(exp.scope)
                        val type = solve.correspondingDeclaration.type
                        val typeId = type.describe()
                        if (type.isArray && exp.nameAsString == "length")
                            mapExpression(exp.scope).length()
                        else {
                            val f = types[typeId]?.asRecordType?.fields?.find { it.id == exp.nameAsString }
                                ?: error("not found", exp) // UnboundVariableDeclaration(exp.nameAsString, procedure)

                            mapExpression(exp.scope).field(f)
                        }
                    }
                }

                is MethodCallExpr -> {
                    //paramTypes = exp.arguments.map { it.calculateResolvedType() }
                    val m = findProcedure(exp.nameAsString, emptyList()) ?: unsupported("not found", exp)
                    val args = exp.arguments.map { mapExpression(it) }
                    if (exp.scope.isPresent)
                        callExpression(m, mapExpression(exp.scope.get()), *args.toTypedArray())
                    else
                        callExpression(m, *args.toTypedArray())
                }
                else -> unsupported("expression", exp)
            }.bind(exp)


        fun handleAssign(block: IBlock, a: AssignExpr): IStatement {

            if (a.target is NameExpr) {
                val target = procedure.variables.find { it.id == a.target.toString() } as? IVariableDeclaration<IBlock>
                    ?: error("not found", a) // UnboundVariableDeclaration(a.target.toString(), block)

                return when (a.operator) {
                    AssignExpr.Operator.ASSIGN -> block.Assign(target, mapExpression(a.value))
                    AssignExpr.Operator.PLUS -> block.Assign(
                        target,
                        ArithmeticOperator.ADD.on(mapExpression(a.target), mapExpression(a.value))
                    )
                    AssignExpr.Operator.MINUS -> block.Assign(
                        target,
                        ArithmeticOperator.SUB.on(mapExpression(a.target), mapExpression(a.value))
                    )
                    AssignExpr.Operator.MULTIPLY -> block.Assign(
                        target,
                        ArithmeticOperator.MUL.on(mapExpression(a.target), mapExpression(a.value))
                    )
                    AssignExpr.Operator.DIVIDE -> block.Assign(
                        target,
                        ArithmeticOperator.DIV.on(mapExpression(a.target), mapExpression(a.value))
                    )
                    AssignExpr.Operator.REMAINDER -> block.Assign(
                        target,
                        ArithmeticOperator.MOD.on(mapExpression(a.target), mapExpression(a.value))
                    )
                    else -> unsupported("assign operator ${a.operator}", a)
                }
            } else if (a.target is ArrayAccessExpr) {
                return when (a.operator) { // TODO compound assignment operators
                    AssignExpr.Operator.ASSIGN ->
                        block.ArraySet(
                            mapExpression((a.target as ArrayAccessExpr).name) as ITargetExpression,
                            mapExpression((a.target as ArrayAccessExpr).index),
                            mapExpression(a.value)
                        )
                    else -> unsupported("assign operator ${a.operator}", a)
                }
            } else if (a.target is FieldAccessExpr) {
                val solve = jpFacade.solve((a.target as FieldAccessExpr).scope)
                val typeId = solve.correspondingDeclaration.type.asReferenceType().id

                return when (a.operator) { // TODO compound assignment operators
                    AssignExpr.Operator.ASSIGN ->
                        block.FieldSet(
                            mapExpression((a.target as FieldAccessExpr).scope) as ITargetExpression,
                            types[typeId]?.asRecordType?.fields?.find { it.id == (a.target as FieldAccessExpr).nameAsString }
                                ?: unsupported("field access", a),
                            mapExpression(a.value)
                        )
                    else -> unsupported("assign operator ${a.operator}", a)
                }
            } else
                unsupported("assignment", a)
        }

        fun handle(block: IBlock, s: Expression): IStatement =
            when (s) {
                is AssignExpr -> handleAssign(block, s)

                is UnaryExpr ->
                    if (s.operator == UnaryExpr.Operator.PREFIX_INCREMENT || s.operator == UnaryExpr.Operator.POSTFIX_INCREMENT) {
                        val dec =
                            procedure.variables.find { it.id == s.expression.toString() } as? IVariableDeclaration<IBlock>
                                ?: unsupported("cannot find variable", s.expression)
                        block.Assign(dec, dec + lit(1))
                    } else if (s.operator == UnaryExpr.Operator.PREFIX_DECREMENT || s.operator == UnaryExpr.Operator.POSTFIX_DECREMENT) {
                        val dec =
                            procedure.variables.find { it.id == s.expression.toString() } as? IVariableDeclaration<IBlock>
                                ?: unsupported("cannot find variable", s.expression)
                        block.Assign(dec, dec - lit(1))
                    } else
                        unsupported("expression statement", s)

                is MethodCallExpr -> {
                    // val paramTypes = s.arguments.map { it.calculateResolvedType() })
                    val m = findProcedure(s.nameAsString, emptyList()) ?: unsupported("not found", s)
                    val args = s.arguments.map { mapExpression(it) }
                    if (s.scope.isPresent)
                        block.Call(m, mapExpression(s.scope.get()), *args.toTypedArray())
                    else
                        block.Call(m, *args.toTypedArray())
                }
                else -> unsupported("expression statement", s)
            }


        val s = when (this) {
            is ReturnStmt -> if (expression.isPresent)
                block.Return(mapExpression(expression.get())).apply {
                    setFlag()
                }
            else
                block.ReturnVoid()

            is IfStmt -> block.If(mapExpression(condition)) {
                thenStmt.translate(procedure, this, procedures, types)
            }.apply {
                if (hasElseBranch()) {
                    createAlternativeBlock { elseBlock ->
                        elseStmt.get().translate(procedure, elseBlock, procedures, types)
                    }
                }
            }

            is WhileStmt -> block.While(mapExpression(this.condition)) {
                body.translate(procedure, this, procedures, types)
            }

            is ForStmt -> block.Block(FOR) {
                initialization.forEach { i ->
                    if (i.isVariableDeclarationExpr) {
                        i.asVariableDeclarationExpr().variables.forEach { v ->
                            val varDec = addVariable(types.mapType(v.type)).apply {
                                id = v.nameAsString
                                setFlag(FOR)
                                bind(i)
                                bind(v.name, ID_LOC)
                                bind(v.type, TYPE_LOC)
                            }
                            if (v.initializer.isPresent)
                                Assign(varDec, mapExpression(v.initializer.get())).apply {
                                    setFlag(FOR)
                                    bind(v)
                                }
                        }
                    } else
                        handle(this, i).bind(i)
                }
                val guard = if (compare.isPresent) mapExpression(compare.get()) else True
                While(guard) {
                    body.translate(procedure, this, procedures, types)
                    update.forEach { u ->
                        handle(this, u).apply {
                            setFlag(FOR)
                            bind(u)
                        }
                    }
                }.apply {
                    setFlag(FOR)
                }
            }

            is ForEachStmt -> block.Block(EFOR) {
                val itVar = addVariable(types.mapType(variable.elementType)).apply {
                    val v = variable.variables[0]
                    id = v.nameAsString
                    setFlag(EFOR)
                    bind(variable)
                    bind(v.name, ID_LOC)
                    bind(v.type, TYPE_LOC)
                }
                val indexVar = addVariable(INT).apply {
                    id = "\$index" // TODO id clash
                    setFlag(EFOR)
                    bind(variable)
                }
                Assign(indexVar, lit(0)).apply {
                    setFlag(EFOR)
                }
                While(indexVar.expression() smaller mapExpression(iterable).length()) {
                    Assign(itVar, mapExpression(iterable).element(indexVar.expression())).apply {
                        setFlag(EFOR)
                    }
                    body.translate(procedure, this, procedures, types)
                    Assign(indexVar, indexVar.expression() plus lit(1)).apply {
                        setFlag(EFOR)
                    }
                }.apply {
                    setFlag(EFOR)
                }
            }

            is BreakStmt -> block.Break()

            is ContinueStmt -> block.Continue()

            is ExpressionStmt ->
                if (expression is VariableDeclarationExpr) {
                    val dec = (expression as VariableDeclarationExpr).variables[0]

                    val type = getType(dec.type)
                    if (type == null)
                        unsupported("type", expression)
                    else {
                        val varDec = block.Var(type, dec.nameAsString)
                        if (dec.initializer.isPresent) {
                            block.Assign(varDec, mapExpression(dec.initializer.get())).bind(this)
                        }
                        varDec.bind(dec.type, TYPE_LOC)
                            .bind(dec.name, ID_LOC)
                    }
                } else
                    handle(block, expression)

            else -> unsupported("statement", this)
        }
        s.bind(this)
    }


    fun mapUnOperator(exp: UnaryExpr): IUnaryOperator =
        when (exp.operator) {
            UnaryExpr.Operator.LOGICAL_COMPLEMENT -> UnaryOperator.NOT
            UnaryExpr.Operator.PLUS -> UnaryOperator.PLUS
            UnaryExpr.Operator.MINUS -> UnaryOperator.MINUS
            else -> unsupported("unary operator", exp)
        }

    fun mapBiOperator(exp: BinaryExpr): IBinaryOperator =
        when (exp.operator) {
            BinaryExpr.Operator.PLUS -> ArithmeticOperator.ADD
            BinaryExpr.Operator.MINUS -> ArithmeticOperator.SUB
            BinaryExpr.Operator.MULTIPLY -> ArithmeticOperator.MUL
            BinaryExpr.Operator.DIVIDE -> ArithmeticOperator.DIV
            BinaryExpr.Operator.REMAINDER -> ArithmeticOperator.MOD

            // TODO IDIV
            // BinaryExpr.Operator.DIVIDE -> ArithmeticOperator.IDIV

            BinaryExpr.Operator.EQUALS -> RelationalOperator.EQUAL
            BinaryExpr.Operator.NOT_EQUALS -> RelationalOperator.DIFFERENT
            BinaryExpr.Operator.LESS -> RelationalOperator.SMALLER
            BinaryExpr.Operator.LESS_EQUALS -> RelationalOperator.SMALLER_EQUAL
            BinaryExpr.Operator.GREATER -> RelationalOperator.GREATER
            BinaryExpr.Operator.GREATER_EQUALS -> RelationalOperator.GREATER_EQUAL

            BinaryExpr.Operator.AND -> LogicalOperator.AND
            BinaryExpr.Operator.OR -> LogicalOperator.OR
            BinaryExpr.Operator.XOR -> LogicalOperator.XOR

            else -> unsupported("binary operator", exp)
        }


}
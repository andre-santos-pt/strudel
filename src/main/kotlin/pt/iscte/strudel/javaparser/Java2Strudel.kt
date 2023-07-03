package pt.iscte.strudel.javaparser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.ProcedureCall
import pt.iscte.strudel.model.impl.RecordFieldExpression
import pt.iscte.strudel.model.impl.VariableExpression
import pt.iscte.strudel.model.util.ArithmeticOperator
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.model.util.UnaryOperator
import java.io.File
import java.util.Optional


const val JP = "JP"
const val FOR = "FOR"
const val EFOR = "EFOR"

const val ID_LOC = "ID_LOC"
const val TYPE_LOC = "TYPE_LOC"
const val OPERATOR_LOC = "OPERATOR_LOC"

private const val THIS_PARAM = "\$this"
private const val INIT = "\$init"
private const val IT = "\$it"


val <T> Optional<T>.getOrNull: T? get() =
    if(isPresent) this.get() else null

fun Optional<Comment>.translateComment(): String? =
    if (isPresent) {
        val comment = get()
        val str = comment.asString()
        str.substring(comment.header?.length ?: 0, str.length - (comment.footer?.length ?: 0)).trim()
    } else null

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

fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) =
    mapType(t.nameAsString)

// inline because otherwise test fails (KotlinNothingValueException)
inline fun unsupported(msg: String, node: Any): Nothing {
    throw AssertionError("unsupported $msg: $node (${node::class.java})")
}

inline fun error(msg: String, node: Any): Nothing {
    throw AssertionError("compile error $msg: $node (${node::class.java})")
}

val CallableDeclaration<*>.body: BlockStmt
    get() = if (this is ConstructorDeclaration) this.body
    else (this as MethodDeclaration).body.get()

fun typeSolver(): TypeSolver {
    val combinedTypeSolver = CombinedTypeSolver()
//    combinedTypeSolver.add(JreTypeSolver())
//        combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
//        combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return combinedTypeSolver
}

val jpFacade = JavaParserFacade.get(typeSolver())



class Java2Strudel(
    val foreignTypes: List<IType> = emptyList(),
    val foreignProcedures: List<IProcedureDeclaration> = defaultForeignProcedures,
    val bindSource: Boolean = true,
    val bindJavaParser: Boolean = true
) {

    private fun <T : IProgramElement> T.bind(
        node: Node,
        prop: String? = null
    ): T {
        if (bindSource && node.range.isPresent) {
            val range = node.range.get()
            val sourceLocation = SourceLocation(
                range.begin.line,
                range.begin.column,
                range.end.column
            )
            if (prop == null)
                setProperty(sourceLocation)
            else
                setProperty(prop, sourceLocation)
        }
        if (bindJavaParser && prop == null)
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

    fun collectHostTypes(
        types: MutableMap<String, IType>,
        type: ClassOrInterfaceDeclaration
    ): List<HostRecordType> {
        val list = mutableListOf<HostRecordType>()
        type.accept(object : VoidVisitorAdapter<Any>() {
            override fun visit(n: VariableDeclarator, arg: Any?) {
                var t = n.type
                while (t.isArrayType)
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

    private val supportedModifiers = listOf(
        Modifier.Keyword.STATIC,
        Modifier.Keyword.PUBLIC,
        Modifier.Keyword.PRIVATE,
    )

    fun translate(classes: List<ClassOrInterfaceDeclaration>) = module {
        val types = defaultTypes.toMutableMap()

        classes.forEach { c ->
            if (c.extendedTypes.isNotEmpty())
                unsupported("extends", c)

            if (c.implementedTypes.isNotEmpty())
                unsupported("implements", c)

            if (c.methods.groupBy { it.nameAsString }.any { it.value.size > 1 })
                unsupported(
                    "method name overload",
                    c.methods.groupBy { it.nameAsString }
                        .filter { it.value.size > 1 })

            //replaceStringConcat(c)

            val type = Record(c.nameAsString) {
                bind(c.name, ID_LOC)
                c.comment.translateComment()?.let { documentation = it }
            }

            types[c.nameAsString] = type.reference()
            types[c.nameAsString + "[]"] = type.array().reference()
            types[c.nameAsString + "[][]"] = type.array().array().reference()
        }

        classes.forEach { c ->
            collectHostTypes(types, c).forEach {
                types[it.id] = it
                types[it.id + "[]"] = it
                types[it.id + "[][]"] = it
            }
        }

        classes.forEach { c ->
            val recordType =
                (types[c.nameAsString] as IReferenceType).target as IRecordType
            c.fields.forEach { f ->
                if (f.variables.size > 1)
                    unsupported("multiple field declarations", f)

                if (f.variables[0].initializer.isPresent)
                    unsupported("field initializers", f)

                recordType.addField(types[f.variables[0].typeAsString]!!) {
                    id = f.variables[0].nameAsString
                }
            }
        }

        fun MethodDeclaration.translateMethod(namespace: String) =
            Procedure(types.mapType(type), nameAsString).apply {
                comment.translateComment()?.let { this.documentation = it }

                if (modifiers.any { !supportedModifiers.contains(it.keyword) })
                    unsupported("modifiers", modifiers)

                setFlag(*modifiers.map { it.keyword.asString() }.toTypedArray())

                if (!modifiers.contains(Modifier.staticModifier()))
                    addParameter(types.mapType((parentNode.get() as ClassOrInterfaceDeclaration))).apply {
                        id = THIS_PARAM
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

        fun ConstructorDeclaration.translateConstructor(namespace: String) =
            Procedure(
                types.mapType(this.nameAsString),
                INIT
            ).apply {
                comment.translateComment()?.let { this.documentation = it }

                if (modifiers.any { !supportedModifiers.contains(it.keyword) })
                    unsupported("modifiers", modifiers)

                setFlag(*modifiers.map { it.keyword.asString() }.toTypedArray())

                addParameter(this.returnType).apply {
                    id = THIS_PARAM
                }
                this@translateConstructor.parameters.forEach { p ->
                    addParameter(types.mapType(p.type)).apply {
                        id = p.nameAsString
                    }.bind(p)
                        .bind(p.type, TYPE_LOC)
                        .bind(p.name, ID_LOC)
                }
                bind(this@translateConstructor)
                setProperty(NAMESPACE_PROP, namespace)
                bind(name, ID_LOC)
                bind(this@translateConstructor.name, TYPE_LOC)
            }

        fun createDefaultConstructor(type: ClassOrInterfaceDeclaration) =
            Procedure(
                types.mapType(type.nameAsString),
                INIT
            ).apply {
                var instance = addParameter(returnType).apply {
                    id = "instance"
                }
                block.Return(instance)
                setProperty(NAMESPACE_PROP, type.nameAsString)
            }

        val procedures: List<Pair<CallableDeclaration<*>?, IProcedure>> =
            classes.filter { it.constructors.isEmpty() && it.methods.none { it.nameAsString == INIT } }
                .map {
                    null to createDefaultConstructor(it)
                } + classes.map { it.constructors }.flatten().map {
                it to it.translateConstructor((it.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString)
            } + classes.map { it.methods }.flatten().map {
                it to it.translateMethod((it.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString)
            }

        procedures.filter {
            it.first != null
        }.forEach {
            it.first!!.body.translate(
                it.second,
                it.second.block,
                procedures,
                types
            )
            if (it.first is ConstructorDeclaration)
                it.second.block.Return(it.second.thisParameter)
        }


    }

    fun List<Pair<CallableDeclaration<*>?, IProcedure>>.findProcedure(
        namespace: String?,
        id: String,
        paramTypes: List<ResolvedType>  // TODO param types in find procedure
    ): IProcedureDeclaration? {
        val find =
            find { (namespace == null || it.second.namespace == namespace) && it.second.id == id }
        if (find != null)
            return find.second

        val findForeign =
            foreignProcedures.find { it.namespace == namespace && it.id == id }
        return findForeign
    }

    fun Statement.translate(
        procedure: IProcedure,
        block: IBlock,
        procedures: List<Pair<CallableDeclaration<*>?, IProcedure>>,
        types: MutableMap<String, IType>
    ) {
        if (this is EmptyStmt)
            return
        else if (this is BlockStmt) {
            this.statements.forEach {
                it.translate(procedure, block, procedures, types)
            }
            return
        }

        fun getType(type: Type): IType =
//            if (type.isArrayType)

//                if(types.containsKey(type.asString()))
//                    types[type.asString()]!!
//                else {
//                    val t = getType(type.elementType).array()
//                    types[type.elementType.asString() + "[]"] = t
//                    t
//                }
//            else
                types[type.asString()]!!

        fun findVariable(id: String): IVariableDeclaration<*>? =
            procedure.variables.find { it.id == id }
                ?: procedure.parameters.find { it.id == THIS_PARAM }
                    ?.let { p ->
                        ((p.type as IReferenceType).target as IRecordType).getField(
                            id
                        )
                    }

        fun mapExpression(exp: Expression): IExpression =
            when (exp) {
                is IntegerLiteralExpr -> lit(exp.value.toInt())
                is DoubleLiteralExpr -> lit(exp.value.toDouble())
                is CharLiteralExpr -> character(exp.value[0])
                is BooleanLiteralExpr -> if (exp.value) True else False

                is NameExpr -> {
                    val target = findVariable(exp.nameAsString)
                    if (target?.isField == true)
                        procedure.thisParameter.field(target as IVariableDeclaration<IRecordType>)
                    else
                        target?.expression() ?: error("not found", exp)
                }

                is ThisExpr -> {
                    procedure.thisParameter.expression()
                }

                is NullLiteralExpr -> NULL_LITERAL

                is StringLiteralExpr -> {
                    foreignProcedures.find { it.id == NEW_STRING }!!
                        .expression(
                            CHAR.array().heapAllocationWith(exp.value.map {
                                CHAR.literal(it)
                            })
                        )
                }

                is UnaryExpr -> mapUnOperator(exp).on(mapExpression(exp.expression))
                    .apply {
                        // TODO review
                        val from = exp.range.get().begin.column
                        val to = exp.expression.range.get().begin.column - 1
                        setProperty(
                            OPERATOR_LOC,
                            SourceLocation(
                                exp.expression.range.get().begin.line,
                                from,
                                to
                            )
                        )
                    }

                is BinaryExpr -> mapBiOperator(exp).on(
                    mapExpression(exp.left),
                    mapExpression(exp.right)
                ).apply {
                    if (exp.left.range.isPresent && exp.right.range.isPresent && exp.left.range.get().begin.line == exp.right.range.get().begin.line) {
                        val from = exp.left.range.get().end.column + 1
                        val to = exp.right.range.get().begin.column - 1
                        setProperty(
                            OPERATOR_LOC,
                            SourceLocation(
                                exp.left.range.get().begin.line,
                                from,
                                to
                            )
                        )
                    }
                }

                is EnclosedExpr -> mapExpression(exp.inner)

                is CastExpr -> UnaryOperator.TRUNCATE.on(mapExpression(exp.expression)) //TODO other casts

                // TODO multi level
                is ArrayCreationExpr -> {
                    if(exp.levels.any { !it.dimension.isPresent })
                        unsupported("multi-dimension array initialization with partial dimensions", exp)

                    val arrayType =
                        types.mapType(exp.elementType.asString()).array()

                    if (exp.levels[0].dimension.isPresent)
                        arrayType.heapAllocation(exp.levels.map {
                            mapExpression(
                                it.dimension.get()
                            )
                        })
                    else
                        mapExpression(exp.initializer.get())
                }

                is ArrayInitializerExpr -> {
                    val values = exp.values.map { mapExpression(it) }
                    val baseType =
                        if (exp.parentNode.getOrNull is ArrayCreationExpr)
                            types.mapType((exp.parentNode.get() as ArrayCreationExpr).elementType)
                        else if (exp.parentNode.getOrNull is VariableDeclarator)
                            types.mapType((exp.parentNode.get() as VariableDeclarator).typeAsString)
                        else
                            unsupported("array initializer", exp)

                    baseType.array().heapAllocationWith(values)
                }

                is ArrayAccessExpr -> mapExpression(exp.name).element(
                    mapExpression(exp.index)
                )

                is ObjectCreationExpr -> {
                    val const = procedures.findProcedure(
                        exp.type.nameAsString,
                        INIT,
                        emptyList()
                    ) // TODO params
                        ?: unsupported("not found", exp)
                    val alloc =
                        types.mapType(exp.type).asRecordType.heapAllocation()
                    const.expression(listOf(alloc) + exp.arguments.map {
                        mapExpression(
                            it
                        )
                    })
                }
                is FieldAccessExpr -> {
                    if (exp.scope is ArrayAccessExpr && exp.nameAsString == "length") {
                        mapExpression(exp.scope).length()
                    } else {
                        if (exp.scope is ThisExpr) {
                            val thisParam =
                                procedure.parameters.find { it.id == THIS_PARAM }!!
                            val thisType =
                                (thisParam.type as IReferenceType).target as IRecordType
                            thisParam.field(thisType.getField(exp.nameAsString)!!)
                        } else {
                            val solve = jpFacade.solve(exp.scope)
                            val type = solve.correspondingDeclaration.type
                            val typeId = type.describe()
                            if (type.isArray && exp.nameAsString == "length")
                                mapExpression(exp.scope).length()
                            else {
                                val f =
                                    types[typeId]?.asRecordType?.fields?.find { it.id == exp.nameAsString }
                                        ?: error(
                                            "not found",
                                            exp
                                        ) // UnboundVariableDeclaration(exp.nameAsString, procedure)

                                mapExpression(exp.scope).field(f)
                            }
                        }
                    }
                }
                is MethodCallExpr -> handleMethodCall(
                    procedure,
                    procedures,
                    types,
                    exp,
                    ::mapExpression
                ) { m, args ->
                    ProcedureCall(NullBlock, m, arguments = args)
                }
                else -> unsupported("expression", exp)
            }.bind(exp)


        fun handleAssign(block: IBlock, a: AssignExpr): IStatement {
            if (a.target is NameExpr) {
                val target = findVariable(a.target.toString())
                    ?: error(
                        "not found",
                        a
                    ) // UnboundVariableDeclaration(a.target.toString(), block)

                val value = when (a.operator) {
                    AssignExpr.Operator.ASSIGN -> mapExpression(a.value)
                    else -> a.operator.map()
                        .on(mapExpression(a.target), mapExpression(a.value))
                }

                return if (target.isField)
                    block.FieldSet(
                        procedure.thisParameter.expression(),
                        target as IVariableDeclaration<IRecordType>,
                        value
                    )
                else
                    block.Assign(target, value)

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
                val solve =
                    jpFacade.solve((a.target as FieldAccessExpr).scope)
                val typeId =
                    solve.correspondingDeclaration.type.asReferenceType().id

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
                    // TODO array ++ --
                    if (s.operator == UnaryExpr.Operator.PREFIX_INCREMENT || s.operator == UnaryExpr.Operator.POSTFIX_INCREMENT) {
                        val varExp = mapExpression(s.expression)
                        if (varExp is VariableExpression)
                            block.Assign(varExp.variable, varExp + lit(1))
                        else if (varExp is RecordFieldExpression)
                            block.FieldSet(
                                varExp.target,
                                varExp.field,
                                varExp + lit(1)
                            )
                        else
                            unsupported("${s.operator} on ${s.expression}", s)
                    } else if (s.operator == UnaryExpr.Operator.PREFIX_DECREMENT || s.operator == UnaryExpr.Operator.POSTFIX_DECREMENT) {
                        val varExp = mapExpression(s.expression)
                        if (varExp is VariableExpression)
                            block.Assign(varExp.variable, varExp - lit(1))
                        else if (varExp is RecordFieldExpression)
                            block.FieldSet(
                                varExp.target,
                                varExp.field,
                                varExp - lit(1)
                            )
                        else
                            unsupported("${s.operator} on ${s.expression}", s)
                    } else
                        unsupported("unary expression statement", s)

                is MethodCallExpr -> {
                    handleMethodCall(
                        procedure,
                        procedures,
                        types,
                        s,
                        ::mapExpression
                    ) { m, args ->
                        ProcedureCall(block, m, arguments = args)
                    }
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
                        elseStmt.get()
                            .translate(
                                procedure,
                                elseBlock,
                                procedures,
                                types
                            )
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
                            if(procedure.variables.any { it.id == v.nameAsString })
                                unsupported("variables with same identifiers within the same procedure", i)
                            val varDec =
                                addVariable(types.mapType(v.type)).apply {
                                    id = v.nameAsString
                                    setFlag(FOR)
                                    bind(i)
                                    bind(v.name, ID_LOC)
                                    bind(v.type, TYPE_LOC)
                                }
                            if (v.initializer.isPresent)
                                Assign(
                                    varDec,
                                    mapExpression(v.initializer.get())
                                ).apply {
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
                if(procedure.variables.any { it.id == variable.variables[0].nameAsString })
                    unsupported("variables with same identifiers within the same procedure", variable)

                val itVar =
                    addVariable(types.mapType(variable.elementType)).apply {
                        val v = variable.variables[0]
                        id = v.nameAsString
                        setFlag(EFOR)
                        bind(variable)
                        bind(v.name, ID_LOC)
                        bind(v.type, TYPE_LOC)
                    }
                val indexVar = addVariable(INT).apply {
                    id = IT + this@Block.depth
                    setFlag(EFOR)
                    bind(variable)
                }
                Assign(indexVar, lit(0)).apply {
                    setFlag(EFOR)
                }
                While(indexVar.expression() smaller mapExpression(iterable).length()) {
                    Assign(
                        itVar,
                        mapExpression(iterable).element(indexVar.expression())
                    ).apply {
                        setFlag(EFOR)
                    }
                    body.translate(procedure, this, procedures, types)
                    Assign(
                        indexVar,
                        indexVar.expression() plus lit(1)
                    ).apply {
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
                    if ((expression as VariableDeclarationExpr).variables.size > 1)
                        unsupported(
                            "multiple variable declarations",
                            expression
                        )

                    val dec =
                        (expression as VariableDeclarationExpr).variables[0]

                    val type = getType(dec.type)
                    if (type == null)
                        unsupported("type", expression)
                    else {
                        if(procedure.variables.any { it.id == dec.nameAsString })
                            unsupported("variables with same identifiers within the same procedure", expression)

                        val varDec = block.Var(type, dec.nameAsString)
                        if (dec.initializer.isPresent) {
                            block.Assign(
                                varDec,
                                mapExpression(dec.initializer.get())
                            ).bind(this)
                        }
                        varDec.bind(dec.type, TYPE_LOC)
                            .bind(dec.name, ID_LOC)
                    }
                } else
                    handle(block, expression)

            else -> unsupported("statement", this)
        }
        comment.translateComment()?.let { s.documentation = it }
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

    fun AssignExpr.Operator.map(): IBinaryOperator =
        when (this) {
            AssignExpr.Operator.PLUS -> ArithmeticOperator.ADD
            AssignExpr.Operator.MINUS -> ArithmeticOperator.SUB
            AssignExpr.Operator.MULTIPLY -> ArithmeticOperator.MUL
            AssignExpr.Operator.DIVIDE -> ArithmeticOperator.DIV
            AssignExpr.Operator.REMAINDER -> ArithmeticOperator.MOD
            else -> unsupported("asign operator", this)
        }

    private fun <T> handleMethodCall(
        procedure: IProcedure,
        procedures: List<Pair<CallableDeclaration<*>?, IProcedure>>,
        types: Map<String, IType>,
        exp: MethodCallExpr,
        mapExpression: (Expression) -> IExpression,
        creator: (IProcedureDeclaration, List<IExpression>) -> T
    ): T {
        // val paramTypes = s.arguments.map { it.calculateResolvedType() })
        val ns = if (exp.scope.isPresent &&
            (exp.scope.get() is NameExpr && types.containsKey(
                exp.scope.get().toString()
            ) ||
                    foreignProcedures.any {
                        it.namespace == exp.scope.get().toString()
                    }
                    )
        )
            exp.scope.get().toString()
        else
            null


        val m = procedures.findProcedure(ns, exp.nameAsString, emptyList())
            ?: unsupported("not found", exp)
        val args = exp.arguments.map { mapExpression(it) }
        return if (exp.scope.isPresent)
            if (ns == null)
                creator(m, listOf(mapExpression(exp.scope.get())) + args)
            else
                creator(m, args)
        else if (m.parameters.isNotEmpty() && m.parameters[0].id == THIS_PARAM)
            creator(m, listOf(procedure.thisParameter.expression()) + args)
        else
            creator(m, args)
    }
}

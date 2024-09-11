package pt.iscte.strudel.parsing.java

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import pt.iscte.strudel.parsing.java.extensions.*
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.PolymophicProcedure
import pt.iscte.strudel.model.util.LogicalOperator
import pt.iscte.strudel.model.util.RelationalOperator
import pt.iscte.strudel.parsing.ITranslator
import pt.iscte.strudel.parsing.java.extensions.matches
import pt.iscte.strudel.parsing.java.extensions.qualifiedName
import java.io.File
import java.util.Locale
import kotlin.reflect.KClass

class StrudelUnsupportedException(msg: String, val nodes: List<Node>) : RuntimeException(msg) {
    val locations = nodes.map { SourceLocation(it) }

    fun getFirstNodeType(): KClass<out Node>? = nodes.firstOrNull()?.let { it::class }

    constructor(msg: String, node: Node) : this(msg, listOf(node))
}

class StrudelCompilationException(message: String) : RuntimeException(message)

@Suppress("NOTHING_TO_INLINE") // inline because otherwise test fails (KotlinNothingValueException)
inline fun unsupported(msg: String, node: Node): Nothing {
    throw StrudelUnsupportedException("Unsupported $msg in: $node", node)
}

@Suppress("NOTHING_TO_INLINE")
inline fun unsupported(msg: String, nodes: List<Node>): Nothing {
    throw StrudelUnsupportedException("Unsupported $msg in:\n\t${nodes.joinToString("\n\t")}", nodes)
}

@Suppress("NOTHING_TO_INLINE")
inline fun error(msg: String, node: Any): Nothing {
    throw StrudelCompilationException("Compilation error at $node (${node::class.java}): $msg")
}

val JPFacade: JavaParserFacade = JavaParserFacade.get(typeSolver())

class Java2Strudel(
    val foreignTypes: List<IType> = emptyList(),
    val foreignProcedures: List<IProcedureDeclaration> = defaultForeignProcedures,
    private val bindSource: Boolean = true,
    private val bindJavaParser: Boolean = true,
    private val preprocessing: CompilationUnit.() -> Unit = { } // Pre-process AST before translating
): ITranslator {

    private val supportedModifiers = listOf(
        Modifier.Keyword.STATIC,
        Modifier.Keyword.PUBLIC,
        Modifier.Keyword.PRIVATE,
    )

    private val fieldInitializers = mutableMapOf<IField, Expression>()

    init {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_20)
        StaticJavaParser.getParserConfiguration().setSymbolResolver(JavaSymbolSolver(typeSolver()))
    }

    override fun load(file: File): IModule {
        val types = StaticJavaParser.parse(file).apply(preprocessing).types

        if (foreignProcedures.isNotEmpty()) // Java wouldn't compile anyway if foreign procedures were being used
            return translate(types)

        val compilation = ClassLoader.compile(file)
        if (compilation.isEmpty())
            return translate(types)
        else
            throw StrudelCompilationException("File $file contains invalid Java code:\n" + compilation.pretty())
    }

    override fun load(files: List<File>): IModule =
        translate(files.flatMap {
            val types = StaticJavaParser.parse(it).apply(preprocessing).types
            if (foreignProcedures.isNotEmpty()) types
            else {
                val compilation = ClassLoader.compile(it)
                if (compilation.isEmpty()) types
                else throw StrudelCompilationException("File $it contains invalid Java code:\n" + compilation.pretty())
            }
        })

    override fun load(src: String): IModule {
        val module = StaticJavaParser.parse(src)
        val types = module.apply(preprocessing).types

        if (foreignProcedures.isNotEmpty())
            return translate(types)

        val compilation = ClassLoader.compile(module.types.first { !it.isPrivate }.nameAsString, src)
        if (compilation.isEmpty() || foreignProcedures.isNotEmpty())
            return translate(types)
        else
            throw StrudelCompilationException("Invalid Java code:\n\n$src\n" + compilation.pretty())
    }

    internal fun <T : IProgramElement> T.bind(
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

            if (this is ILoop) {
                val len = when(node) {
                    is WhileStmt -> 4
                    is ForStmt, is ForEachStmt -> 2
                    is DoStmt -> 1
                    else -> 0
                }
                setProperty(KEYWORD_LOC, SourceLocation(range.begin.line, range.begin.column,  range.begin.column + len))
            }
        }
        if (bindJavaParser && prop == null)
            setProperty(JP, node)
        return this
    }

    /**
     * Finds a procedure with matching namespace, ID, and parameter types.
     * @receiver A list of (JavaParser Declaration, IProcedureDeclaration) pairs.
     * @param namespace Procedure namespace.
     * @param id Procedure ID.
     * @param paramTypes List of parameter types.
     * @return The matching procedure declaration, or null if one wasn't found.
     */
    internal fun List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>.findProcedure(
        namespace: String?,
        id: String,
        paramTypes: List<IType>
    ): IProcedureDeclaration? {
        // println("Finding procedure $namespace.$id(${paramTypes.joinToString { it.id!! }})")
        val find = find { it.second.matches(namespace, id, paramTypes) }
        return find?.second ?: foreignProcedures.find { it.matches(namespace, id, paramTypes) }
    }

    internal fun <T> handleMethodCall(
        procedure: IProcedure,
        procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
        types: Map<String, IType>,
        exp: MethodCallExpr,
        mapExpression: (Expression) -> IExpression,
        invoke: (IProcedureDeclaration, List<IExpression>) -> T
    ): T {
        val paramTypes: List<IType> = exp.arguments.map { it.getResolvedIType(types) }

        // Get method namespace
        val namespace: Namespace? = exp.getNamespace(types, foreignProcedures)

        // Find matching procedure declaration
        val method =
            procedures.findProcedure(namespace?.qualifiedName, exp.nameAsString, paramTypes) ?: exp.asForeignProcedure(
                procedure.module!!,
                namespace?.qualifiedName,
                types
            ) ?: error(
                "procedure matching method call $exp not found within namespace ${namespace?.qualifiedName}",
                exp
            )

        // Get method call arguments
        val args = exp.arguments.map { mapExpression(it) }

        return if (exp.scope.isPresent) {
            if (namespace == null || !namespace.isStatic) { // No namespace or instance method
                val thisParam = mapExpression(exp.scope.get())
                invoke(method, listOf(thisParam) + args)
            } else invoke(method, args) // Static method
        } else if (method.parameters.isNotEmpty() && method.parameters[0].id == THIS_PARAM) {
            val thisParam = procedure.thisParameter.expression()
            invoke(method, listOf(thisParam) + args)
        } else invoke(method, args)
    }

    private fun collectHostTypes(
        types: MutableMap<String, IType>,
        type: ClassOrInterfaceDeclaration
    ): List<HostRecordType> {
        val list = mutableListOf<HostRecordType>()
        type.accept(object : VoidVisitorAdapter<Any>() {
            override fun visit(n: VariableDeclarator, arg: Any?) {
                var t = n.type
                while (t.isArrayType)
                    t = t.asArrayType().componentType
                val typeName = kotlin.runCatching { t.resolve().erasure().describe() }.getOrDefault(t.asString())
                if (typeName !in types)
                    runCatching { getClassByName(typeName) }.onSuccess { list.add(HostRecordType(it.canonicalName)) }
            }
        }, null)
        return list
    }

    /**
     * Translates a list of JavaParser type declarations to a Strudel [IModule].
     * @param typeDeclarations A list of JavaParser type declarations.
     * @return A Strudel [IModule].
     */
    private fun translate(typeDeclarations: List<TypeDeclaration<*>>) = module {
        val types = defaultTypes.toMutableMap()

        // Collect all types beforehand (to have something to bind to later, if needed)
        (typeDeclarations + typeDeclarations.flatMap { it.nestedTypes }).forEach {
            val type = Record(it.qualifiedName) {
                bind(it.name, ID_LOC)
                it.comment.translateComment()?.let { c -> documentation = c }
            }
            types[it.qualifiedName] = type.reference()
            types["${it.qualifiedName}[]"] = type.array().reference()
            types["${it.qualifiedName}[][]"] = type.array().array().reference()
        }

        fun IProcedureDeclaration.setPropertiesAndBind(callable: CallableDeclaration<*>, namespace: String) {
            // Check for unsupported modifiers
            if (callable.modifiers.any { !supportedModifiers.contains(it.keyword) })
                unsupported("modifiers", callable.modifiers.filter { !supportedModifiers.contains(it.keyword) })

            // Set modifiers
            setFlag(*callable.modifiers.map { it.keyword.asString() }.toTypedArray())

            // Set namespace
            setProperty(NAMESPACE_PROP, namespace)

            // Translate comment
            callable.comment.translateComment()?.let { this.documentation = it }

            // Bind
            bind(callable.name, ID_LOC)
            bind(callable)
            if (callable is MethodDeclaration) bind(callable.type, TYPE_LOC)
            else bind(callable.name, TYPE_LOC)
        }

        /**
         * Translates a JavaParser method declaration into a Strudel procedure declaration.
         * @receiver Any JavaParser method declaration.
         * @param namespace Method namespace.
         * @return IProcedure with correct modifiers and parameters but empty body.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : TypeDeclaration<*>> MethodDeclaration.translateMethodDeclaration(namespace: String): IProcedureDeclaration =
            if (!body.isPresent)
                PolymophicProcedure(this@module, namespace, nameAsString, types.mapType(type)).apply {
                    setPropertiesAndBind(this@translateMethodDeclaration, namespace)

                    // Interface methods ""are always instance methods"" (don't quote us on this) -> add $this parameter
                    addParameter(types.mapType((parentNode.get() as T))).apply { id = THIS_PARAM }

                    // Add regular method parameters
                    this@translateMethodDeclaration.parameters.forEach { p ->
                        addParameter(types.mapType(p.type)).apply { id = p.nameAsString }
                            .bind(p)
                            .bind(p.type, TYPE_LOC)
                            .bind(p.name, ID_LOC)
                    }
                }
            else
                Procedure(types.mapType(type), nameAsString).apply {
                    setPropertiesAndBind(this@translateMethodDeclaration, namespace)

                    // Is instance method --> Add $this parameter
                    if (!modifiers.contains(Modifier.staticModifier()))
                        addParameter(types.mapType((parentNode.get() as T))).apply { id = THIS_PARAM }

                    // Add regular method parameters
                    this@translateMethodDeclaration.parameters.forEach { p ->
                        addParameter(types.mapType(p.type)).apply { id = p.nameAsString }
                            .bind(p)
                            .bind(p.type, TYPE_LOC)
                            .bind(p.name, ID_LOC)
                    }
                }

        /**
         * Translates a Java Parser constructor declaration.
         * @receiver Java Parser constructor declaration.
         * @param declaringClass Declaration of declaring class.
         * @return IProcedure with correct modifiers and parameters but empty body.
         */
        fun ConstructorDeclaration.translateConstructorDeclaration(declaringClass: ClassOrInterfaceDeclaration): IProcedureDeclaration =
            Procedure(
                types.mapType(runCatching { resolve().declaringType().qualifiedName }.getOrDefault(nameAsString)),
                INIT
            ).apply {
                setPropertiesAndBind(this@translateConstructorDeclaration, declaringClass.qualifiedName)
                setFlag(CONSTRUCTOR_FLAG)

                // Add $outer parameter if inner class
                var outer: IParameter? = null
                if (declaringClass.isInnerClass) {
                    val parent = declaringClass.parentNode.get() as ClassOrInterfaceDeclaration
                    outer = addParameter(types.mapType(parent.qualifiedName)).apply { id = OUTER_PARAM }
                }

                // Add $this parameter
                val instance = addParameter(this.returnType).apply { id = THIS_PARAM }

                // Set field initialiser for $outer parameter
                if (outer != null && returnType.asRecordType.getField(OUTER_PARAM) != null) {
                    block.FieldSet( // s.f = e
                        instance.expression(),
                        returnType.asRecordType.getField(OUTER_PARAM)!!,
                        outer.expression()
                    )
                }

                // Add regular constructor parameters
                this@translateConstructorDeclaration.parameters.forEach { p ->
                    addParameter(types.mapType(p.type)).apply { id = p.nameAsString }
                        .bind(p)
                        .bind(p.type, TYPE_LOC)
                        .bind(p.name, ID_LOC)
                }
            }

        /**
         * Creates a default constructor for a JavaParser type declaration.
         * @param type JavaParser type declaration.
         * @return IProcedure with `return $this` statement (if class declaration) and otherwise empty body.
         */
        fun createDefaultConstructor(type: TypeDeclaration<*>): IProcedureDeclaration =
            Procedure(
                types.mapType(type.qualifiedName),
                INIT
            ).apply {
                setFlag("public")
                setFlag(CONSTRUCTOR_FLAG)
                setProperty(NAMESPACE_PROP, type.qualifiedName)

                // Default record constructor needs to include parameters.
                // Default class constructor is totally empty, e.g. public ClassName() { }.
                if (type is RecordDeclaration) {
                    addParameter(returnType).apply { id = THIS_PARAM }
                    type.parameters.forEach {p ->
                        addParameter(types.mapType(p.type)).apply { id = p.nameAsString }
                            .bind(p)
                            .bind(p.type, TYPE_LOC)
                            .bind(p.name, ID_LOC)
                    }
                }
            }

        /**
         * Collects the declarations (i.e. empty body) of all the declared procedures within a JavaParser type
         * declaration.
         * @receiver A JavaParser type declaration (e.g. class or record declaration).
         * @return A list of (JavaParser Declaration, IProcedure) pairs. Declaration is null for default constructors.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : TypeDeclaration<*>> TypeDeclaration<T>.getProcedureDeclarations(): List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> {
            val defaultConstructors =
                if (this is RecordDeclaration || (this is ClassOrInterfaceDeclaration && !isInterface && constructors.isEmpty() && methods.none { it.nameAsString == INIT }))
                    listOf(null to createDefaultConstructor(this))
                else listOf()

            val explicitConstructors =
                if (this is ClassOrInterfaceDeclaration && constructors.isNotEmpty())
                    constructors.map { it to it.translateConstructorDeclaration(it.parentNode.get() as ClassOrInterfaceDeclaration) }
                else listOf()

            val methods = methods.map {
                it.replaceStringPlusWithConcat()
                it.substituteControlBlocks()
                it.replaceIncDecAsExpressions()
                it to it.translateMethodDeclaration<T>((it.parentNode.get() as T).qualifiedName)
            }

            return defaultConstructors + explicitConstructors + methods
        }

        // Collect all procedures beforehand (to have something to bind procedure calls to, if needed)
        // Bodies are translated later as needed
        val proceduresPerType: Map<TypeDeclaration<*>, List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>> =
            (typeDeclarations + typeDeclarations.flatMap { it.nestedTypes }).associateWith { it.getProcedureDeclarations() }
        val procedureDeclarations = proceduresPerType.values.flatten()

        /**
         * Injects field assignment statements in a procedure based on a record type's field initializers.
         * @receiver Any IProcedure. The goal is to use this with record type constructors.
         * @param type Record type. If null, defaults to procedure's return type as a record type.
         */
        fun IProcedure.injectFieldInitializers(type: IRecordType? = null) {
            val exp2Strudel = JavaExpression2Strudel(this, block, procedureDeclarations, types, this@Java2Strudel, mutableMapOf())
            val t = type ?: thisParameter.type.asRecordType
            t.fields.reversed().forEach { field ->
                fieldInitializers[field]?.let {
                    block.FieldSet(thisParameter.expression(), field, exp2Strudel.map(it), 0)
                }
            }
        }

        /**
         * Translates a list of JavaParser class or interface declarations to the current Strudel module.
         * @param classes The list of class or interface declarations to translate.
         */
        fun translateClassAndInterfaceDeclarations(classes: List<ClassOrInterfaceDeclaration>) {
            val allClasses = classes + classes.flatMap { it.nestedTypes }.filterIsInstance<ClassOrInterfaceDeclaration>()

            allClasses.forEach { c ->
                if (c.extendedTypes.isNotEmpty())
                    unsupported("extends keyword", c.extendedTypes.first())

                /* FIXME method overloading should be OK unless signatures actually match
                if (c.methods.groupBy { it.nameAsString }.any { it.value.size > 1 }) {
                    val nodes = c.methods
                        .groupBy { it.nameAsString }
                        .filter { it.value.size > 1 }
                        .values.first()
                        .toList()
                    unsupported("method name overloading", nodes)
                }
                 */

                val type = (types.mapType(c.qualifiedName) as IReferenceType).target as IRecordType
                if (c.isInnerClass) {
                    val parent = c.parentNode.get() as ClassOrInterfaceDeclaration
                    type.addField(types.mapType(parent.qualifiedName)) { id = OUTER_PARAM }
                }
            }

            // Translate field declarations for each class
            allClasses.filter { !it.isInterface }.forEach { c ->
                val recordType = (types[c.qualifiedName] as IReferenceType).target as IRecordType

                val defaultConstructor: IProcedure? = proceduresPerType[c]!!.firstOrNull {
                    it.first == null && it.second.namespace == c.qualifiedName && it.second.hasFlag(CONSTRUCTOR_FLAG)
                }?.second as? IProcedure

                if (defaultConstructor != null) {
                    var outer: IParameter? = null
                    if (c.isInnerClass) {
                        val parent = c.parentNode.get() as ClassOrInterfaceDeclaration
                        outer = defaultConstructor.addParameter(types.mapType(parent.qualifiedName)).apply { id = OUTER_PARAM }
                    }
                    val instance = defaultConstructor.addParameter(defaultConstructor.returnType).apply { id = THIS_PARAM }
                    if (outer != null)
                        defaultConstructor.block.FieldSet(
                            instance.expression(),
                            defaultConstructor.returnType.asRecordType.getField(OUTER_PARAM)!!,
                            outer!!.expression()
                        )
                    defaultConstructor.block.Return(instance)
                }

                c.fields.forEach { field ->
                    field.variables.forEach { variableDeclaration ->
                        val fieldType =
                            if (variableDeclaration.isGeneric(c)) types["java.lang.Object"]
                            else types[variableDeclaration.typeAsString]
                                ?: types[kotlin.runCatching { variableDeclaration.type.resolve().describe() }
                                    .getOrDefault(variableDeclaration.type.asString())]
                        if (fieldType == null)
                            error(
                                "Could not find type for variable declaration ${variableDeclaration.typeAsString} / ${
                                    variableDeclaration.type.resolve().describe()}", variableDeclaration
                            )

                        val f = recordType.addField(fieldType) { id = variableDeclaration.nameAsString }

                        // Store field initializer JavaParser expressions
                        // (these are translated and injected into constructor(s) later)
                        if (variableDeclaration.initializer.isPresent)
                            fieldInitializers[f] = variableDeclaration.initializer.get()
                    }
                }
            }

            // Get all procedures in a list of class declarations
            val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> =
                allClasses.flatMap { proceduresPerType[it] ?: listOf() }

            // Translate bodies of all procedures
            procedures.forEach {
                if (it.first != null && it.second is IProcedure) {
                    val stmtTranslator = JavaStatement2Strudel(it.second as IProcedure, procedureDeclarations, types, this@Java2Strudel)

                    // Translate statements in declaration body
                    it.first!!.body?.let { body -> stmtTranslator.translate(body, (it.second as IProcedure).block) }

                    // Inject field initializers and return statement in constructors
                    if (it.first is ConstructorDeclaration) {
                        (it.second as IProcedure).injectFieldInitializers()
                        (it.second as IProcedure).block.Return(it.second.thisParameter)
                    }
                } else if (it.first == null) // No JavaParser declaration --> default constructor
                    (it.second as IProcedure).injectFieldInitializers() // Inject field initializers into default constructors
            }
        }

        /**
         * Translates a list of JavaParser record declarations to the current Strudel module.
         * @param records The list of records declarations to translate.
         */
        fun translateRecordDeclarations(records: List<RecordDeclaration>) {
            records.forEach { r ->
                val type = (types.mapType(r.qualifiedName) as IReferenceType).target as IRecordType

                if (r.constructors.isNotEmpty())
                    unsupported("record with explicit constructors", r)

                // Check if record has 1 compact constructor
                val compact: CompactConstructorDeclaration? =
                    if (r.compactConstructors.isEmpty()) null
                    else if (r.compactConstructors.size == 1) r.compactConstructors.first()
                    else unsupported("record with multiple compact constructors", r)

                // Get default constructor
                val constructor = proceduresPerType[r]!!.first {
                    it.second.namespace == r.qualifiedName && it.second.hasFlag(CONSTRUCTOR_FLAG)
                }.second as IProcedure

                val fieldSetters = mutableListOf<Pair<IField, ITargetExpression>>()
                r.parameters.forEach { param ->
                    val paramType =
                        if (param.isGeneric(r)) types["java.lang.Object"]
                        else types[param.typeAsString]
                            ?: types[kotlin.runCatching { param.type.resolve().describe() }
                                .getOrDefault(param.type.asString())]
                    if (paramType == null)
                        error(
                            "Could not find type for record parameter declaration ${param.typeAsString} / ${
                                param.type.resolve().describe()}", param
                        )

                    // Add type field
                    val field = type.addField(paramType) { id = param.nameAsString }
                    val constructorParam = constructor.parameters.first { it.type == field.type && it.id == field.id }
                    fieldSetters.add(Pair(field, constructorParam.expression()))

                    // Generate get() method for field
                    Procedure(types.mapType(param.type), param.nameAsString).apply {
                        setFlag("public")
                        setProperty(NAMESPACE_PROP, r.qualifiedName)
                        val t = addParameter(constructor.returnType).apply { id = THIS_PARAM }
                        block.Return(t.field(field))
                    }
                }

                val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> =
                    proceduresPerType[r]!!.filter { !it.second.hasFlag(CONSTRUCTOR_FLAG) }

                // Translate internal procedures
                procedures.forEach {
                    if (it.first != null && it.second is IProcedure) {
                        val stmtTranslator = JavaStatement2Strudel(it.second as IProcedure, procedureDeclarations, types, this@Java2Strudel)
                        it.first!!.body?.let { body -> stmtTranslator.translate(body, (it.second as IProcedure).block) }
                    }
                }

                // If type has a compact constructor, inject statements into default constructor
                if (compact != null) {
                    compact.replaceStringPlusWithConcat()
                    compact.substituteControlBlocks()
                    compact.replaceIncDecAsExpressions()
                    val stmtTranslator = JavaStatement2Strudel(constructor, procedureDeclarations, types, this@Java2Strudel)
                    stmtTranslator.translate(compact.body, constructor.block)
                }

                // Set fields in default constructor and return instance
                fieldSetters.forEach { (field, expression) ->
                    constructor.block.FieldSet(constructor.thisParameter, field, expression)
                }
                constructor.block.Return(constructor.thisParameter)

                // Generate equals() for record type
                Procedure(BOOLEAN, "equals").apply {
                    setFlag("public")
                    setFlag(EQUALS_FLAG)
                    setProperty(NAMESPACE_PROP, r.qualifiedName)
                    val self = addParameter(constructor.returnType).apply { id = THIS_PARAM }
                    val other = addParameter(constructor.returnType).apply { id = "other" }
                    var exp: ICompositeExpression? = null
                    type.fields.forEach {
                        val field = self.field(it)
                        val otherField = other.field(field.field)
                        val comparison =
                            if (field.type.isRecordReference && (field.type as IReferenceType).target.asRecordType.hasEquals)
                                field.type.asRecordType.equals.expression(field, otherField)
                            else if (otherField.type.isRecordReference && (other.type as IReferenceType).target.asRecordType.hasEquals)
                                (other.type as IReferenceType).target.asRecordType.equals.expression(field, otherField)
                            else
                                RelationalOperator.EQUAL.on(field, otherField)
                        exp = if (exp == null) comparison else LogicalOperator.AND.on(exp!!, comparison)
                    }
                    block.Return(exp ?: False)
                }
            }
        }

        /**
         * Translates a list of JavaParser enum declarations to the current Strudel module.
         * @param enums The list of enum declarations to translate.
         */
        fun translateEnumDeclarations(enums: List<EnumDeclaration>) {
            if (enums.isNotEmpty())
                unsupported("enum keyword", enums.first())
        }

        /**
         * Translates a list of JavaParser annotations declarations to the current Strudel module.
         * @param annotations The list of annotation declarations to translate.
         */
        fun translateAnnotationDeclarations(annotations: List<AnnotationDeclaration>) {
            if (annotations.isNotEmpty())
                unsupported("annotations", annotations.first())
        }

        translateClassAndInterfaceDeclarations(typeDeclarations.filterIsInstance<ClassOrInterfaceDeclaration>())

        translateRecordDeclarations(typeDeclarations.filterIsInstance<RecordDeclaration>())

        translateEnumDeclarations(typeDeclarations.filterIsInstance<EnumDeclaration>())

        translateAnnotationDeclarations(typeDeclarations.filterIsInstance<AnnotationDeclaration>())
    }
}

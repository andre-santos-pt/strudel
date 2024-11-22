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
import pt.iscte.strudel.parsing.java.extensions.matches
import pt.iscte.strudel.parsing.java.extensions.qualifiedName
import java.io.File
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class Java2Strudel(
    val foreignProcedures: List<IProcedureDeclaration> = defaultForeignProcedures,
    private val bindSource: Boolean = true,
    private val bindJavaParser: Boolean = true,
    private val checkJavaCompilation: Boolean = true,
    private val preprocessing: CompilationUnit.() -> Unit = { }, // Pre-process AST before translating
    private val allowedForeignNamespaces: List<String> = listOf(
        java.lang.Integer::class.java.canonicalName,
        java.lang.Double::class.java.canonicalName,
        java.lang.Character::class.java.canonicalName,
        java.lang.Boolean::class.java.canonicalName,
        String::class.java.canonicalName,
        Math::class.java.canonicalName,
        Random::class.java.canonicalName,
        Comparator::class.java.canonicalName,
        Comparable::class.java.canonicalName,
        Arrays::class.java.canonicalName,
        Collections::class.java.canonicalName,
        List::class.java.canonicalName,
        ArrayList::class.java.canonicalName
        )
    ) {
    internal val JPFacade: JavaParserFacade = JavaParserFacade.get(typeSolver())

    private val supportedModifiers = listOf(
        Modifier.Keyword.STATIC,
        Modifier.Keyword.PUBLIC,
        Modifier.Keyword.PRIVATE,
    )

    private val fieldInitializers = mutableMapOf<IField, Expression>()

    init {
        StaticJavaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_20)
        StaticJavaParser.getParserConfiguration()
            .setSymbolResolver(JavaSymbolSolver(typeSolver()))
    }

    // if className is empty, src cannot have public classes
    fun load(src: String, className: String): IModule {
        if (checkJavaCompilation) {
            val diagnostics = ClassLoader.compile(className, src)
            if (diagnostics.isNotEmpty())
                LoadingError.compilation(diagnostics)
        }

        val compilationUnit = StaticJavaParser.parse(src)
        val types = compilationUnit.apply(preprocessing).types
        val module = translate(types)
        JavaParserFacade.clearInstances()
        return module
    }

    // src cannot have public classes
    fun load(src: String): IModule =
        load(src, "")

    private fun javaCompile(src: String, className: String) {
        if (checkJavaCompilation) {
            val diagnostics = ClassLoader.compile(className, src)
            if (diagnostics.isNotEmpty())
                LoadingError.compilation(diagnostics)
        }
    }

    fun load(file: File): IModule =
        load(file.readText(), file.nameWithoutExtension)

    fun load(files: List<File>): IModule =
        translate(files.flatMap {
            val src = it.readText()
            javaCompile(src, it.nameWithoutExtension)
            val compilationUnit = StaticJavaParser.parse(src)
            compilationUnit.apply(preprocessing).types
        })


    internal fun <T : IProgramElement> T.bind(
        node: Node,
        prop: String? = null
    ): T {
        if (bindSource && node.range.isPresent) {
            val range = node.range.get()

            val sourceLocation = SourceLocation(node)
            if (prop == null)
                setProperty(sourceLocation)
            else
                setProperty(prop, sourceLocation)

            if (this is ILoop) {
                val len = when (node) {
                    is WhileStmt -> 4
                    is ForStmt, is ForEachStmt -> 2
                    is DoStmt -> 1
                    else -> 0
                }
                setProperty(
                    KEYWORD_LOC,
                    SourceLocation(
                        range.begin.line,
                        range.end.line,
                        range.begin.column,
                        range.begin.column + len,
                        len
                    )
                )
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
        val find = find { it.second.matches(namespace, id, paramTypes) }
        return find?.second ?: foreignProcedures.find {
            it.matches(
                namespace,
                id,
                paramTypes
            )
        }
    }

    internal fun <T> handleMethodCall(
        procedure: IProcedure,
        procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>,
        types: Map<String, IType>,
        exp: MethodCallExpr,
        mapExpression: (Expression) -> IExpression,
        invoke: (IProcedureDeclaration, List<IExpression>) -> T
    ): T {
        val paramTypes: List<IType> =
            exp.arguments.map { it.getResolvedIType(types) }

        // Get method namespace
        val namespace: Namespace? = exp.getNamespace(types, foreignProcedures)

        // Find matching procedure declaration
        val method =
            procedures.findProcedure(
                namespace?.qualifiedName,
                exp.nameAsString,
                paramTypes
            ) ?:
            (if(namespace?.qualifiedName in allowedForeignNamespaces)
                exp.asForeignProcedure(
                procedure.module!!,
                namespace?.qualifiedName,
                types
            ) else null)
                ?: LoadingError.translation(
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
                val typeName = kotlin.runCatching {
                    t.resolve().erasure().simpleNameAsString
                }.getOrDefault(t.asString())
                if (typeName !in types)
                    runCatching {
                        getClassByName(
                            typeName,
                            n
                        )
                    }.onSuccess { list.add(HostRecordType(it.canonicalName)) }
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
        members.addAll(foreignProcedures)

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

        fun IProcedureDeclaration.setPropertiesAndBind(
            callable: CallableDeclaration<*>,
            namespace: String
        ) {
            // Check for unsupported modifiers
            if (callable.modifiers.any { !supportedModifiers.contains(it.keyword) })
                LoadingError.unsupported(
                    callable.modifiers.filter { !supportedModifiers.contains(it.keyword) }
                        .map {
                            Pair(
                                "modifier ${it.keyword.asString()}",
                                SourceLocation(it)
                            )
                        })

            // Set modifiers
            setFlag(*callable.modifiers.map { it.keyword.asString() }
                .toTypedArray())

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
        fun <T : TypeDeclaration<*>> MethodDeclaration.translateMethodDeclaration(
            namespace: String
        ): IProcedureDeclaration =
            if (!body.isPresent)
                PolymophicProcedure(
                    this@module,
                    namespace,
                    nameAsString,
                    types.mapType(type, this@translateMethodDeclaration)
                ).apply {
                    setPropertiesAndBind(
                        this@translateMethodDeclaration,
                        namespace
                    )

                    // Interface methods ""are always instance methods"" (don't quote us on this) -> add $this parameter
                    addParameter(
                        types.mapType(
                            parentNode.get() as T,
                            this@translateMethodDeclaration
                        )
                    ).apply { id = THIS_PARAM }

                    // Add regular method parameters
                    this@translateMethodDeclaration.parameters.forEach { p ->
                        addParameter(types.mapType(p.type, p)).apply {
                            id = p.nameAsString
                        }
                            .bind(p)
                            .bind(p.type, TYPE_LOC)
                            .bind(p.name, ID_LOC)
                    }
                }
            else
                Procedure(
                    types.mapType(type, this@translateMethodDeclaration),
                    nameAsString
                ).apply {
                    setPropertiesAndBind(
                        this@translateMethodDeclaration,
                        namespace
                    )

                    // Is instance method --> Add $this parameter
                    if (!modifiers.contains(Modifier.staticModifier()))
                        addParameter(
                            types.mapType(
                                parentNode.get() as T,
                                this@translateMethodDeclaration
                            )
                        ).apply { id = THIS_PARAM }

                    // Add regular method parameters
                    this@translateMethodDeclaration.parameters.forEach { p ->
                        addParameter(types.mapType(p.type, p)).apply {
                            id = p.nameAsString
                        }
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
        fun ConstructorDeclaration.translateConstructorDeclaration(
            declaringClass: ClassOrInterfaceDeclaration
        ): IProcedureDeclaration =
            Procedure(
                types.mapType(
                    runCatching { resolve().declaringType().qualifiedName }.getOrDefault(
                        nameAsString
                    ),
                    this@translateConstructorDeclaration
                ),
                INIT
            ).apply {
                setPropertiesAndBind(
                    this@translateConstructorDeclaration,
                    declaringClass.qualifiedName
                )
                setFlag(CONSTRUCTOR_FLAG)

                // Add $outer parameter if inner class
                var outer: IParameter? = null
                if (declaringClass.isInnerClass) {
                    val parent =
                        declaringClass.parentNode.get() as ClassOrInterfaceDeclaration
                    outer = addParameter(
                        types.mapType(
                            parent.qualifiedName,
                            this@translateConstructorDeclaration
                        )
                    ).apply { id = OUTER_PARAM }
                }

                // Add $this parameter
                val instance =
                    addParameter(this.returnType).apply { id = THIS_PARAM }

                // Set field initialiser for $outer parameter
                if (outer != null && returnType.asRecordType.getField(
                        OUTER_PARAM
                    ) != null
                ) {
                    block.FieldSet( // s.f = e
                        instance.expression(),
                        returnType.asRecordType.getField(OUTER_PARAM)!!,
                        outer.expression()
                    )
                }

                // Add regular constructor parameters
                this@translateConstructorDeclaration.parameters.forEach { p ->
                    addParameter(types.mapType(p.type, p)).apply {
                        id = p.nameAsString
                    }
                        .bind(p)
                        .bind(p.type, TYPE_LOC)
                        .bind(p.name, ID_LOC)
                }
            }

        fun Parameter.translateRecordField(
            declaringRecord: RecordDeclaration
        ): IProcedureDeclaration = Procedure(
            types.mapType(type, this),
            nameAsString
        ).apply {
            setFlag("public")
            setProperty(NAMESPACE_PROP, declaringRecord.qualifiedName)
            val recordType = types.mapType(
                declaringRecord,
                this@translateRecordField
            )
            addParameter(recordType).apply {
                id = THIS_PARAM
            }
        }

        fun RecordDeclaration.translateRecordEquals(): IProcedureDeclaration =
            Procedure(
                BOOLEAN,
                "equals"
            ).apply {
                setFlag("public")
                setProperty(
                    NAMESPACE_PROP,
                    this@translateRecordEquals.qualifiedName
                )
                val recordType = types.mapType(
                    this@translateRecordEquals,
                    this@translateRecordEquals
                )
                addParameter(recordType).apply {
                    id = THIS_PARAM
                }
                addParameter(recordType).apply {
                    id = "other"
                }
            }

        /**
         * Creates a default constructor for a JavaParser type declaration.
         * @param type JavaParser type declaration.
         * @return IProcedure with `return $this` statement (if class declaration) and otherwise empty body.
         */
        fun createDefaultConstructor(type: TypeDeclaration<*>): IProcedureDeclaration =
            Procedure(
                types.mapType(type.qualifiedName, type),
                INIT
            ).apply {
                setFlag("public")
                setFlag(CONSTRUCTOR_FLAG)
                setProperty(NAMESPACE_PROP, type.qualifiedName)

                // Default record constructor needs to include parameters.
                // Default class constructor is totally empty, e.g. public ClassName() { }.
                if (type is RecordDeclaration) {
                    addParameter(returnType).apply { id = THIS_PARAM }
                    type.parameters.forEach { p ->
                        addParameter(types.mapType(p.type, p)).apply {
                            id = p.nameAsString
                        }
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
                    constructors.map {
                        it to it.translateConstructorDeclaration(
                            it.parentNode.get() as ClassOrInterfaceDeclaration
                        )
                    }
                else listOf()

            val methods = methods.map {
                it.replaceBinaryOperatorAssignWithRegularAssign()
                it.replaceStringPlusWithConcat(types)
                it.substituteControlBlocks()
                it.replaceIncDecAsExpressions()
                it to it.translateMethodDeclaration<T>((it.parentNode.get() as T).qualifiedName)
            }

            val implicitRecordMethods =
                if (this is RecordDeclaration)
                    parameters.map {
                        null to it.translateRecordField(this)
                    } + listOf(null to this.translateRecordEquals())
                else
                    listOf()

            return defaultConstructors + explicitConstructors + methods + implicitRecordMethods
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
            val exp2Strudel = JavaExpression2Strudel(
                this,
                block,
                procedureDeclarations,
                types,
                this@Java2Strudel,
                mutableMapOf()
            )
            val t = type ?: thisParameter.type.asRecordType
            t.fields.reversed().forEach { field ->
                fieldInitializers[field]?.let {
                    block.FieldSet(
                        thisParameter.expression(),
                        field,
                        exp2Strudel.map(it),
                        0
                    )
                }
            }
        }

        /**
         * Translates a list of JavaParser class or interface declarations to the current Strudel module.
         * @param classes The list of class or interface declarations to translate.
         */
        fun translateClassAndInterfaceDeclarations(classes: List<ClassOrInterfaceDeclaration>) {
            val allClasses = classes + classes.flatMap { it.nestedTypes }
                .filterIsInstance<ClassOrInterfaceDeclaration>()

            allClasses.forEach { c ->
                if (c.extendedTypes.isNotEmpty())
                    LoadingError.unsupported(
                        "extends keyword",
                        c.extendedTypes.first()
                    )

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

                val type = (types.mapType(
                    c.qualifiedName,
                    c
                ) as IReferenceType).target as IRecordType
                if (c.isInnerClass) {
                    val parent =
                        c.parentNode.get() as ClassOrInterfaceDeclaration
                    type.addField(types.mapType(parent.qualifiedName, c)) {
                        id = OUTER_PARAM
                    }
                }
            }

            // Translate field declarations for each class
            allClasses.filter { !it.isInterface }.forEach { c ->
                val recordType =
                    (types[c.qualifiedName] as IReferenceType).target as IRecordType

                val defaultConstructor: IProcedure? =
                    proceduresPerType[c]?.firstOrNull {
                        it.first == null && it.second.namespace == c.qualifiedName && it.second.hasFlag(
                            CONSTRUCTOR_FLAG
                        )
                    }?.second as? IProcedure

                if (defaultConstructor != null) {
                    var outer: IParameter? = null
                    if (c.isInnerClass) {
                        val parent =
                            c.parentNode.get() as ClassOrInterfaceDeclaration
                        outer = defaultConstructor.addParameter(
                            types.mapType(
                                parent.qualifiedName,
                                c
                            )
                        ).apply { id = OUTER_PARAM }
                    }
                    val instance =
                        defaultConstructor.addParameter(defaultConstructor.returnType)
                            .apply { id = THIS_PARAM }
                    if (outer != null && defaultConstructor.returnType.asRecordType.getField(
                            OUTER_PARAM
                        ) != null
                    )
                        defaultConstructor.block.FieldSet(
                            instance.expression(),
                            defaultConstructor.returnType.asRecordType.getField(
                                OUTER_PARAM
                            )!!,
                            outer!!.expression()
                        )
                    defaultConstructor.block.Return(instance)
                }

                c.fields.forEach { field ->
                    field.variables.forEach { variableDeclaration ->
                        val fieldType =
                            if (variableDeclaration.isGeneric(c)) types["java.lang.Object"]
                            else types[variableDeclaration.typeAsString]
                                ?: types[kotlin.runCatching { variableDeclaration.type.resolve().simpleNameAsString }
                                    .getOrDefault(variableDeclaration.type.asString())]
                        if (fieldType == null)
                            LoadingError.translation(
                                "Could not find type for variable declaration ${variableDeclaration.typeAsString} / ${
                                    variableDeclaration.type.resolve().simpleNameAsString
                                }", variableDeclaration
                            )

                        val f = recordType.addField(fieldType) {
                            id = variableDeclaration.nameAsString
                        }.bind(field.variables.first().name, ID_LOC)

                        // Store field initializer JavaParser expressions
                        // (these are translated and injected into constructor(s) later)
                        if (variableDeclaration.initializer.isPresent)
                            fieldInitializers[f] =
                                variableDeclaration.initializer.get()
                    }
                }
            }

            // Get all procedures in a list of class declarations
            val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> =
                allClasses.flatMap { proceduresPerType[it] ?: listOf() }

            // Translate bodies of all procedures
            procedures.forEach {
                if (it.first != null && it.second is IProcedure) {
                    val stmtTranslator = JavaStatement2Strudel(
                        it.second as IProcedure,
                        procedureDeclarations,
                        types,
                        this@Java2Strudel
                    )

                    // Translate statements in declaration body
                    it.first?.body?.let { body ->
                        stmtTranslator.translate(
                            body,
                            (it.second as IProcedure).block
                        )
                    }

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
                val type = (types.mapType(
                    r.qualifiedName,
                    r
                ) as IReferenceType).target as IRecordType

                if (r.constructors.isNotEmpty())
                    LoadingError.unsupported(
                        "record with explicit constructors",
                        r
                    )

                // Check if record has 1 compact constructor
                val compact: CompactConstructorDeclaration? =
                    if (r.compactConstructors.isEmpty()) null
                    else if (r.compactConstructors.size == 1) r.compactConstructors.first()
                    else LoadingError.unsupported(
                        "record with multiple compact constructors",
                        r
                    )

                // Get default constructor
                val constructor = proceduresPerType[r]?.first {
                    it.second.namespace == r.qualifiedName && it.second.hasFlag(
                        CONSTRUCTOR_FLAG
                    )
                }?.second as? IProcedure

                if (constructor != null) {
                    val fieldSetters =
                        mutableListOf<Pair<IField, ITargetExpression>>()
                    r.parameters.forEach { param ->
                        val paramType =
                            if (param.isGeneric(r)) types["java.lang.Object"]
                            else types[param.typeAsString]
                                ?: types[kotlin.runCatching { param.type.resolve().simpleNameAsString }
                                    .getOrDefault(param.type.asString())]
                        if (paramType == null)
                            LoadingError.translation(
                                "Could not find type for record parameter declaration ${param.typeAsString} / ${
                                    param.type.resolve().simpleNameAsString
                                }", param
                            )

                        // Add type field
                        val field =
                            type.addField(paramType) { id = param.nameAsString }
                                .bind(param.name, ID_LOC)
                        val constructorParam =
                            constructor.parameters.first { it.type == field.type && it.id == field.id }
                        fieldSetters.add(
                            Pair(
                                field,
                                constructorParam.expression()
                            )
                        )

                        // implicit field body
                        proceduresPerType[r]?.find { it.first == null && it.second.id == param.nameAsString }
                            ?.let {
                                val p = it.second as IProcedure
                                p.block.Return(
                                    p.thisParameter.field(
                                        type.getField(
                                            param.nameAsString
                                        )!!
                                    )
                                )
                            }
                    }

                    val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>>? =
                        proceduresPerType[r]?.filter {
                            !it.second.hasFlag(
                                CONSTRUCTOR_FLAG
                            )
                        }

                    // Translate internal procedures
                    procedures?.forEach {
                        if (it.first != null && it.second is IProcedure) {
                            val stmtTranslator = JavaStatement2Strudel(
                                it.second as IProcedure,
                                procedureDeclarations,
                                types,
                                this@Java2Strudel
                            )
                            it.first?.body?.let { body ->
                                stmtTranslator.translate(
                                    body,
                                    (it.second as IProcedure).block
                                )
                            }
                        }
                    }

                    // If type has a compact constructor, inject statements into default constructor
                    if (compact != null) {
                        compact.replaceBinaryOperatorAssignWithRegularAssign()
                        compact.replaceStringPlusWithConcat(types)
                        compact.substituteControlBlocks()
                        compact.replaceIncDecAsExpressions()
                        val stmtTranslator = JavaStatement2Strudel(
                            constructor,
                            procedureDeclarations,
                            types,
                            this@Java2Strudel
                        )
                        stmtTranslator.translate(
                            compact.body,
                            constructor.block
                        )
                    }

                    // Set fields in default constructor and return instance
                    fieldSetters.forEach { (field, expression) ->
                        constructor.block.FieldSet(
                            constructor.thisParameter,
                            field,
                            expression
                        )
                    }
                    constructor.block.Return(constructor.thisParameter)

                    constructor.setProperty(JP, compact) // TODO

                    // Generate equals() for record type
                    (proceduresPerType[r]?.find { it.first == null && it.second.id == "equals" }?.second
                            as? IProcedure)?.apply {
                        val self = parameters[0]
                        val other = parameters[1]
                        //other.expression().instanceOf(type)
                        //this@apply.block.If(other.)
                        var exp: ICompositeExpression? = null
                        type.fields.forEach {
                            val field = self.field(it)
                            val otherField = other.field(field.field)
                            val comparison =
                                if (field.type.isRecordReference && (field.type as IReferenceType).target.asRecordType.hasEquals)
                                    field.type.asRecordType.equals.expression(
                                        field,
                                        otherField
                                    )
                                else if (otherField.type.isRecordReference && (other.type as IReferenceType).target.asRecordType.hasEquals)
                                    (other.type as IReferenceType).target.asRecordType.equals.expression(
                                        field,
                                        otherField
                                    )
                                else
                                    RelationalOperator.EQUAL.on(
                                        field,
                                        otherField
                                    )
                            exp =
                                if (exp == null) comparison else LogicalOperator.AND.on(
                                    exp!!,
                                    comparison
                                )
                        }
                        this@apply.block.Return(exp ?: False)
                    }
                }
            }
        }

        /**
         * Translates a list of JavaParser enum declarations to the current Strudel module.
         * @param enums The list of enum declarations to translate.
         */
        fun translateEnumDeclarations(enums: List<EnumDeclaration>) {
            if (enums.isNotEmpty())
                LoadingError.unsupported(
                    "enum declarations",
                    enums.first().name
                )
        }

        /**
         * Translates a list of JavaParser annotations declarations to the current Strudel module.
         * @param annotations The list of annotation declarations to translate.
         */
        fun translateAnnotationDeclarations(annotations: List<AnnotationDeclaration>) {
            if (annotations.isNotEmpty())
                LoadingError.unsupported(
                    "annotation declarations",
                    annotations.first().name
                )
        }

        translateClassAndInterfaceDeclarations(typeDeclarations.filterIsInstance<ClassOrInterfaceDeclaration>())

        translateRecordDeclarations(typeDeclarations.filterIsInstance<RecordDeclaration>())

        translateEnumDeclarations(typeDeclarations.filterIsInstance<EnumDeclaration>())

        translateAnnotationDeclarations(typeDeclarations.filterIsInstance<AnnotationDeclaration>())
    }
}

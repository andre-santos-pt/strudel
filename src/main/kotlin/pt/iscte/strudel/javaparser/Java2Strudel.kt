package pt.iscte.strudel.javaparser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.*
import pt.iscte.strudel.model.impl.PolymophicProcedure
import java.io.File

import pt.iscte.strudel.javaparser.extensions.*

class StrudelUnsupportedException(msg: String, val nodes: List<Node>) : RuntimeException(msg) {
    val locations = nodes.map { SourceLocation(it) }

    constructor(msg: String, node: Node) : this(msg, listOf(node))
}

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
    throw AssertionError("Compilation error at $node (${node::class.java}): $msg")
}

val JPFacade: JavaParserFacade = JavaParserFacade.get(typeSolver())


class Java2Strudel(
    val foreignTypes: List<IType> = emptyList(),
    val foreignProcedures: List<IProcedureDeclaration> = defaultForeignProcedures,
    private val bindSource: Boolean = true,
    private val bindJavaParser: Boolean = true
) {

    private val supportedModifiers = listOf(
        Modifier.Keyword.STATIC,
        Modifier.Keyword.PUBLIC,
        Modifier.Keyword.PRIVATE,
    )

    private val fieldInitializers = mutableMapOf<IField, Expression>()

    init {
        StaticJavaParser.getParserConfiguration().setSymbolResolver(JavaSymbolSolver(typeSolver()))
    }

    fun load(file: File): IModule =
        translate(StaticJavaParser.parse(file).types.filterIsInstance<ClassOrInterfaceDeclaration>())

    fun load(files: List<File>): IModule = translate(files.map {
        StaticJavaParser.parse(it).types.filterIsInstance<ClassOrInterfaceDeclaration>()
    }.flatten())

    fun load(src: String): IModule =
        translate(StaticJavaParser.parse(src).types.filterIsInstance<ClassOrInterfaceDeclaration>())

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
        }
        if (bindJavaParser && prop == null)
            setProperty(JP, node)
        return this
    }

    private fun IProcedureDeclaration.matches(namespace: String?, id: String, parameterTypes: List<IType>): Boolean {
        val paramTypeMatch = this.parameters.map { it.type } == parameterTypes // FIXME
        val idAndNamespaceMatch =
            if (namespace == null) this.id == id
            else this.namespace == namespace && this.id == id
        return idAndNamespaceMatch // && paramTypeMatch
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
        paramTypes: List<IType>  // TODO param types in find procedure
    ): IProcedureDeclaration? {
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
        val scope: String? =
            if (namespace == null || namespace.isAbstract) null
            else namespace.qualifiedName

        // Find matching procedure declaration
        val method =
            procedures.findProcedure(scope, exp.nameAsString, paramTypes) ?:
            exp.asForeignProcedure(types) ?:
            error("procedure matching method call $exp not found within namespace ${namespace?.qualifiedName}", exp)

        // Get method call arguments
        val args = exp.arguments.map { mapExpression(it) }

        return if (exp.scope.isPresent) {
            if (namespace == null || !namespace.isStatic) { // No namespace or instance method
                val thisParam = mapExpression(exp.scope.get())
                invoke(method, listOf(thisParam) + args)
            }
            else invoke(method, args) // Static method
        }
        else if (method.parameters.isNotEmpty() && method.parameters[0].id == THIS_PARAM) {
            val thisParam = procedure.thisParameter.expression()
            invoke(method, listOf(thisParam) + args)
        }
        else invoke(method, args)
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

    private fun translate(classes: List<ClassOrInterfaceDeclaration>) = module {
        val types = defaultTypes.toMutableMap()

        /**
         * Translates a JavaParser method declaration into a Strudel procedure declaration.
         * @receiver Any JavaParser method declaration.
         * @param namespace Method namespace.
         * @return IProcedure with correct modifiers and parameters but empty body.
         */
        fun MethodDeclaration.translateMethod(namespace: String): IProcedureDeclaration =
            if (!body.isPresent)
                PolymophicProcedure(this@module, namespace, nameAsString, types.mapType(type)).apply {
                    comment.translateComment()?.let { this.documentation = it }

                    // Check for unsupported modifiers
                    if (modifiers.any { !supportedModifiers.contains(it.keyword) })
                        unsupported("modifiers", modifiers.filter { !supportedModifiers.contains(it.keyword) })

                    // Add modifiers
                    setFlag(*modifiers.map { it.keyword.asString() }.toTypedArray())

                    // Interface methods ""are always instance methods"" (don't quote us on this) -> add $this parameter
                    addParameter(types.mapType((parentNode.get() as ClassOrInterfaceDeclaration))).apply {
                        id = THIS_PARAM
                    }

                    // Add regular method parameters
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
            else
                Procedure(types.mapType(type), nameAsString).apply {
                    comment.translateComment()?.let { this.documentation = it }

                    // Check for unsupported modifiers
                    if (modifiers.any { !supportedModifiers.contains(it.keyword) })
                        unsupported("modifiers", modifiers.filter { !supportedModifiers.contains(it.keyword) })

                    // Add modifiers
                    setFlag(*modifiers.map { it.keyword.asString() }.toTypedArray())

                    // Is instance method --> Add $this parameter
                    if (!modifiers.contains(Modifier.staticModifier()))
                        addParameter(types.mapType((parentNode.get() as ClassOrInterfaceDeclaration))).apply {
                            id = THIS_PARAM
                        }

                    // Add regular method parameters
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

        /**
         * Translates a Java Parser constructor declaration.
         * @receiver Java Parser constructor declaration.
         * @param namespace Constructor namespace.
         * @return IProcedure with correct modifiers and parameters but empty body.
         */
        fun ConstructorDeclaration.translateConstructor(namespace: String) =
            Procedure(
                types.mapType(this.nameAsString),
                INIT
            ).apply {
                comment.translateComment()?.let { this.documentation = it }

                // Check for unsupported modifiers
                if (modifiers.any { !supportedModifiers.contains(it.keyword) })
                    unsupported("modifiers", modifiers.filter { !supportedModifiers.contains(it.keyword) })

                // Set modifiers and constructor flag
                setFlag(*modifiers.map { it.keyword.asString() }.toTypedArray())
                setFlag(CONSTRUCTOR_FLAG)

                // Add $this parameter
                addParameter(this.returnType).apply {
                    id = THIS_PARAM
                }

                // Add regular constructor parameters
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

        /**
         * Creates a default constructor for a JavaParser class or interface declaration.
         * @param type JavaParser class declaration.
         * @return IProcedure with `return $this` statement and otherwise empty body.
         */
        fun createDefaultConstructor(type: ClassOrInterfaceDeclaration) =
            Procedure(
                types.mapType(type.nameAsString),
                INIT
            ).apply {
                setFlag("public")
                setFlag(CONSTRUCTOR_FLAG)
                val instance = addParameter(returnType).apply { id = THIS_PARAM }
                block.Return(instance)
                setProperty(NAMESPACE_PROP, type.nameAsString)
            }

        classes.forEach { c ->
            if (c.extendedTypes.isNotEmpty())
                unsupported("extends keyword", c.extendedTypes.first())

            if (c.methods.groupBy { it.nameAsString }.any { it.value.size > 1 }) {
                val nodes = c.methods
                    .groupBy { it.nameAsString }
                    .filter { it.value.size > 1 }
                    .values.first()
                    .toList()
                unsupported("method name overloading", nodes)
            }

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

        // Translate field declarations for each class
        classes.filter { !it.isInterface }.forEach { c ->
            val recordType = (types[c.nameAsString] as IReferenceType).target as IRecordType
            c.fields.forEach { field ->
                field.variables.forEach { variableDeclaration ->
                    val f = recordType.addField(types[variableDeclaration.typeAsString]!!) {
                        id = variableDeclaration.nameAsString
                    }

                    // Store field initializer JavaParser expressions
                    // (these are translated and injected into constructor(s) later)
                    if (variableDeclaration.initializer.isPresent)
                        fieldInitializers[f] = variableDeclaration.initializer.get()
                }
            }
        }

        /**
         * Associates every callable declaration in a list of class declarations with a Strudel IProcedureDeclaration.
         * @receiver A list of JavaParser class or interface declarations.
         * @return A list of (JavaParser Declaration, IProcedure) pairs. Declaration is null for default constructors.
         */
        fun List<ClassOrInterfaceDeclaration>.getAllProcedures(): List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> {
            // Default constructors created for classes with no explicit constructor declarations
            val defaultConstructors = filter {
                !it.isInterface && it.constructors.isEmpty() && it.methods.none { method -> method.nameAsString == INIT }
            }.map {
                null to createDefaultConstructor(it)
            }

            // Each explicit constructor declarations are translated
            val explicitConstructors = flatMap { it.constructors }.map {
                it to it.translateConstructor((it.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString)
            }

            // Each of a type's methods is translated
            val methods = flatMap { it.methods }.map {
                it to it.translateMethod((it.parentNode.get() as ClassOrInterfaceDeclaration).nameAsString)
            }

            return defaultConstructors + explicitConstructors + methods
        }

        // Get all procedures in a list of class declarations
        val procedures: List<Pair<CallableDeclaration<*>?, IProcedureDeclaration>> = classes.getAllProcedures()

        /**
         * Injects field assignment statements in a procedure based on a record type's field initializers.
         * @receiver Any IProcedure. The goal is to use this with record type constructors.
         * @param type Record type. If null, defaults to procedure's return type as a record type.
         */
        fun IProcedure.injectFieldInitializers(type: IRecordType? = null) {
            val exp2Strudel = JavaExpression2Strudel(this, block, procedures, types, this@Java2Strudel)
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

        // Translate bodies of all procedures
        procedures.forEach {
            if (it.first != null && it.second is IProcedure) {
                val stmtTranslator = JavaStatement2Strudel(
                    it.second as IProcedure,
                    procedures,
                    types,
                    this@Java2Strudel
                )

                // Translate statements in declaration body
                it.first!!.body?.let { body -> stmtTranslator.translate(body, (it.second as IProcedure).block) }

                // Inject field initializers and return statement in constructors
                if (it.first is ConstructorDeclaration) {
                    (it.second as IProcedure).injectFieldInitializers()
                    (it.second as IProcedure).block.Return(it.second.thisParameter)
                }
            } else if (it.first == null) { // No JavaParser declaration --> default constructor
                // Inject field initializers into default constructors
                (it.second as IProcedure).injectFieldInitializers()
            }
        }
    }
}

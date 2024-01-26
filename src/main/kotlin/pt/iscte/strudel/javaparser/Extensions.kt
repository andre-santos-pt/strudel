package pt.iscte.strudel.javaparser

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.types.ResolvedArrayType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Value
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.util.*

fun getStringValue(str: String): IValue = Value(stringType, java.lang.String(str))

val IModule.proceduresExcludingConstructors: List<IProcedure>
    get() = procedures.filter { it.id != INIT }

val <T> Optional<T>.getOrNull: T?
    get() =
        if (isPresent) this.get() else null

fun Optional<Comment>.translateComment(): String? =
    if (isPresent) {
        val comment = get()
        val str = comment.asString()
        str.substring(comment.header?.length ?: 0, str.length - (comment.footer?.length ?: 0)).trim()
    } else null

fun getType(t: String): IType = when (t) {
    "int", "long" -> INT // TODO: hacky
    "double", "float" -> DOUBLE // TODO: hacky
    "boolean" -> BOOLEAN
    "char" -> CHAR
    "String" -> stringType
    else -> try {
        JavaType(Class.forName(t))
    } catch (e1: Exception) {
        try {
            JavaType(Class.forName("java.lang.$t"))
        } catch (e2: Exception) {
            try {
                JavaType(Class.forName("java.util.$t"))
            } catch (e3: Exception) {
                error("unsupported type $t", t)
            }
        }
    }
}

fun getClass(name: String): Class<*> =
    try {
        Class.forName(name)
    } catch (e1: Exception) {
        try {
            Class.forName("java.lang.$name")
        } catch (e2: Exception) {
            try {
                Class.forName("java.util.$name")
            } catch (e3: Exception) {
                error("unsupported type $name", name)
            }
        }
    }

fun isJavaClassName(name: String): Boolean = runCatching { getClass(name) }.isSuccess

fun MutableMap<String, IType>.mapType(t: String): IType =
    if (containsKey(t))
        this[t]!!
    else getType(t)

fun MutableMap<String, IType>.mapType(t: Type) =
    if (t is ClassOrInterfaceType)
        mapType(t.nameAsString)
    else
        mapType(t.asString())

fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) =
    mapType(t.nameAsString)

val CallableDeclaration<*>.body: BlockStmt?
    get() = if (this is ConstructorDeclaration) this.body else (this as MethodDeclaration).body.getOrNull

fun typeSolver(): TypeSolver {
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }
}

internal fun createForeignProcedure(scope: String?, method: Method, isStatic: Boolean): ForeignProcedure =
    ForeignProcedure(
        null,
        scope,
        method.name,
        getType(method.returnType.typeName),
        (if (isStatic) listOf() else listOf(getType(method.declaringClass.simpleName))) + method.parameters.map { getType(it.type.typeName) }
    )
    { vm, args ->
        val res =
            if (isStatic) // static --> no caller
                method.invoke(null, *args.map { it.value }.toTypedArray())
            else if (args.size == 1) { // not static --> caller is $this parameter
                method.invoke(args.first().value)
            } else if (args.size > 1) {
                val caller = args.first().value // should be $this
                if (caller!!.javaClass != method.declaringClass)
                    error("Cannot invoke instance method $method with object instance $caller: is ${caller.javaClass.canonicalName}, should be ${method.declaringClass.canonicalName}")
                val arguments = args.slice(1 until args.size).map { it.value }
                method.invoke(caller, *arguments.toTypedArray())
            } else error("Cannot invoke instance method $method with 0 arguments: missing (at least) object instance")
        when (res) {
            is String -> getStringValue(res)
            else -> vm.getValue(res)
        }
    }

internal fun ResolvedType.toIType(types: Map<String, IType>): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> types[this.qualifiedName] ?: HostRecordType(this.qualifiedName)
    is ResolvedArrayType -> ArrayType(componentType.toIType(types))
    // is LazyType -> this.concrete
    else -> error("unsupported expression type $this", this)
}

internal fun ResolvedType.toJavaType(): Class<*> = when (this) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClass(this.id)
    is ResolvedArrayType -> Array.newInstance(this.componentType.toJavaType(), 0).javaClass // TODO: test
    else -> error("unsupported expression type $this", this)
}

internal fun Expression.getResolvedJavaType(): Class<*> = calculateResolvedType().toJavaType()

internal fun Expression.getResolvedIType(types: Map<String, IType>): IType = calculateResolvedType().toIType(types)

internal val MethodCallExpr.isAbstractMethodCall: Boolean
    get() = resolve().isAbstract

internal val MethodCallExpr.isStaticMethodCall: Boolean
    get() = resolve().isStatic

data class MethodNamespace(val qualifiedName: String, val type: NamespaceType, val isStatic: Boolean)
enum class NamespaceType { CLASS, ABSTRACT_REFERENCE, CONCRETE_REFERENCE }
internal fun MethodCallExpr.getNamespace(types: Map<String, IType>, foreignProcedures: List<IProcedureDeclaration>): MethodNamespace? =
    if (scope.isPresent) {
        val scope = scope.get()

        val scopeIsCurrentlyLoadedType: Boolean = scope is NameExpr && types.containsKey(scope.toString())
        val scopeIsForeignProcedure: Boolean = foreignProcedures.any { it.namespace == scope.toString() }
        val scopeIsValidJavaClass: Boolean = isJavaClassName(scope.toString())

        if (scopeIsCurrentlyLoadedType || scopeIsForeignProcedure || scopeIsValidJavaClass)
            MethodNamespace(scope.toString(), NamespaceType.CLASS, false)
        else if (isAbstractMethodCall) when (val type = scope.calculateResolvedType()) {
            is ResolvedReferenceType -> MethodNamespace(type.qualifiedName, NamespaceType.ABSTRACT_REFERENCE, isStaticMethodCall)
            else -> null
        } else MethodNamespace(scope.getResolvedJavaType().canonicalName, NamespaceType.CONCRETE_REFERENCE, isStaticMethodCall)
    } else null


internal fun MethodCallExpr.asForeignProcedure(): IProcedureDeclaration? {
    if (isAbstractMethodCall) return null
    if (scope.isPresent) {
        val namespace = scope.get()
        if (namespace is NameExpr) {
            val clazz: Class<*> = runCatching {
                getClass(namespace.toString())
            }.getOrNull() ?: namespace.getResolvedJavaType()

            val args = arguments.map { it.getResolvedJavaType() }.toTypedArray()
            val method: Method = clazz.getMethod(nameAsString, *args)

            val isStaticMethod = this.resolve().isStatic
            return createForeignProcedure(namespace.toString(), method, isStaticMethod)
        } else
            unsupported("automatic foreign procedure creation for method call expression: $this", this)
    }
    return null
}
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
import pt.iscte.strudel.vm.impl.ForeignProcedure
import java.lang.reflect.Method
import java.util.*

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

internal fun createForeignProcedure(scope: String?, method: Method): ForeignProcedure =
    ForeignProcedure(
        null,
        scope,
        method.name,
        getType(method.returnType.typeName),
        method.parameters.map { getType(it.type.typeName) }
    )
    { vm, args ->
        println("Calling ${method.name}(${args.joinToString { it.type.toString() }})")
        vm.getValue(method.invoke(null, *args.map { it.value }.toTypedArray()))
    }

internal fun ResolvedType.toIType(): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> TODO("Reference type resolution to Strudel IType not yet implemented")
    is ResolvedArrayType -> ArrayType(componentType.toIType())
    else -> error("unsupported expression type $this", this)
}

internal fun Expression.getResolvedJavaType(): Class<*> = when (val type = calculateResolvedType()) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClass(type.id)
    is ResolvedArrayType -> TODO("Array type resolution to Java Class not yet implemented")
    else -> error("unsupported expression type $type", this)
}

internal fun Expression.getResolvedIType(): IType = calculateResolvedType().toIType()

internal fun MethodCallExpr.asForeignProcedure(): IProcedureDeclaration? {
    if (scope.isPresent) {
        val namespace = scope.get()
        if (namespace is NameExpr) { // todo does this work for instance methods? (I feel like it doesn't)
            val clazz: Class<*> = getClass(namespace.toString())
            val method: Method = clazz.getMethod(nameAsString, *arguments.map { it.getResolvedJavaType() }.toTypedArray())
            println("Method for $this is $method")
            return createForeignProcedure(namespace.toString(), method)
        } else unsupported("automatic foreign procedure creation for method call expression $this", this)
    }
    return null
}
package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.model.typesystem.LazyType
import com.github.javaparser.resolution.model.typesystem.NullType
import com.github.javaparser.resolution.types.ResolvedArrayType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.resolution.types.ResolvedTypeVariable
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.javaparser.JavaType
import pt.iscte.strudel.javaparser.defaultTypes
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.vm.IValue
import java.lang.reflect.Array
import java.lang.reflect.Method
import kotlin.jvm.optionals.getOrNull

internal fun typeSolver(): TypeSolver {
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }
}

internal fun MutableMap<String, IType>.mapType(t: String): IType = this[t] ?: getTypeByName(t, this)

internal fun MutableMap<String, IType>.mapType(t: Type) = mapType(kotlin.runCatching { t.resolve().erasure().describe() }.getOrDefault(t.asString()))

internal fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) =
    mapType(t.fullyQualifiedName.getOrNull() ?: t.nameAsString)

internal fun isJavaClassName(qualifiedName: String): Boolean = runCatching { getClassByName(qualifiedName) }.isSuccess

internal fun getTypeByName(qualifiedName: String, types: Map<String, IType> = defaultTypes): IType {
    //println("Getting type with name $qualifiedName from [${types.keys.joinToString()}]")
    return defaultTypes[qualifiedName] ?: types[qualifiedName] ?: JavaType(getClassByName(qualifiedName))
}
/*
    try {
        JavaType(Class.forName(name))
    } catch (e1: Exception) {
        try {
            JavaType(Class.forName("java.lang.$name"))
        } catch (e2: Exception) {
            try {
                JavaType(Class.forName("java.util.$name"))
            } catch (e3: Exception) {
                pt.iscte.strudel.javaparser.error("could not find IType with name $name", name)
            }
        }
    }
 */

internal fun getClassByName(qualifiedName: String): Class<*> {
    val arrayTypeDepth = Regex("\\[\\]").findAll(qualifiedName).count()

    if (arrayTypeDepth == 0)
        return runCatching { Class.forName(qualifiedName) }.getOrNull() ?:
        pt.iscte.strudel.javaparser.error("unsupported class $qualifiedName", qualifiedName)

    // TODO could also do array types for IType with .array().reference() instead of .arrayType()
    return runCatching {
        val componentTypeName = qualifiedName.replace("[]", "")
        var cls = Class.forName(componentTypeName)
        (0 until arrayTypeDepth).forEach { _ -> cls = cls.arrayType() }
        cls
    }.getOrNull() ?: pt.iscte.strudel.javaparser.error("unsupported class $qualifiedName", qualifiedName)
}

internal fun getTypeFromJavaParser(node: Node, type: Type, types: Map<String, IType>): IType =
    runCatching { getTypeByName(type.resolve().erasure().describe(), types) }.getOrNull() ?:
    runCatching { getTypeByName(type.asString(), types) }.getOrNull() ?:
    pt.iscte.strudel.javaparser.error("could not find IType for type $type", node)

internal fun ResolvedType.toIType(types: Map<String, IType>): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> getTypeByName(this.qualifiedName, types)
    is ResolvedArrayType -> ArrayType(componentType.toIType(types))
    is LazyType -> getTypeByName(this.erasure().describe(), types)
    is ResolvedTypeVariable -> types["java.lang.Object"]!!
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type ${this::class.qualifiedName}", this)
}

internal fun ResolvedType.toJavaType(): Class<*> = when (this) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClassByName(this.qualifiedName)
    is ResolvedArrayType -> Array.newInstance(this.componentType.toJavaType(), 0).javaClass
    is LazyType -> getClassByName(this.describe())
    is ResolvedTypeVariable -> Any::class.java // Generics compile to Object
    is NullType -> Nothing::class.java
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type ${this::class.qualifiedName}", this)
}

internal fun Expression.getResolvedJavaType(): Class<*> = kotlin.runCatching {
    calculateResolvedType().toJavaType()
}.onFailure { println("Failed to resolve Java type of $this: $it") }.getOrThrow()

internal fun Expression.getResolvedIType(types: Map<String, IType>): IType = kotlin.runCatching {
    calculateResolvedType().toIType(types)
}.onFailure { println("Failed to resolve type of $this: $it")  }.getOrThrow()

internal fun Class<*>.findCompatibleMethod(name: String, parameterTypes: Iterable<Class<*>>): Method? =
    methods.find {
        it.name == name && it.parameterTypes.zip(parameterTypes).all {
            p -> p.first == p.second || p.second.isAssignableFrom(p.first)
        }
    }

internal fun IType.toJavaType(): Class<*> = when(this) {
    is IReferenceType -> target.toJavaType()
    is IArrayType -> Array.newInstance(this.componentType.toJavaType(), 0).javaClass
    is JavaType -> this.type
    is HostRecordType -> this.type
    INT -> Int::class.java
    DOUBLE -> Double::class.java
    BOOLEAN -> Boolean::class.java
    CHAR -> Char::class.java
    VOID -> Void::class.java
    else -> error("IType $this has no matching Java type")
}

// Trying collapsing into a single "when" branch, but it didn't work the same
internal val Class<*>.wrapperType: Class<*>
    get() = when (this) {
        Int::class.java -> Int::class.javaObjectType
        Long::class.java -> Long::class.javaObjectType
        Double::class.java -> Double::class.javaObjectType
        Float::class.java -> Float::class.javaObjectType
        Char::class.java -> Char::class.javaObjectType
        Byte::class.java -> Byte::class.javaObjectType
        Boolean::class.java -> Boolean::class.javaObjectType
        else -> this
    }

internal val Class<*>.primitiveType: Class<*>
    get() = when (this) {
        Int::class.javaObjectType -> Int::class.java
        Long::class.javaObjectType -> Long::class.java
        Double::class.javaObjectType -> Double::class.java
        Float::class.javaObjectType -> Float::class.java
        Char::class.javaObjectType -> Char::class.java
        Byte::class.javaObjectType -> Byte::class.java
        Boolean::class.javaObjectType -> Boolean::class.java
        else -> this
    }

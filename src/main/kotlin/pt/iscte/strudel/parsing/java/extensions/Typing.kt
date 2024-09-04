package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.model.typesystem.LazyType
import com.github.javaparser.resolution.model.typesystem.NullType
import com.github.javaparser.resolution.types.*
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.parsing.java.OUTER_PARAM
import pt.iscte.strudel.parsing.java.defaultTypes
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.vm.NULL
import java.lang.reflect.Array
import java.lang.reflect.Method
import kotlin.jvm.optionals.getOrNull
import pt.iscte.strudel.parsing.java.error

internal fun typeSolver(): TypeSolver = CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }

internal fun MutableMap<String, IType>.mapType(t: String): IType = this[t] ?: getTypeByName(t, this)

internal fun MutableMap<String, IType>.mapType(t: Type) =
    mapType(kotlin.runCatching { t.resolve().erasure().describe() }.getOrDefault(t.asString()))

internal fun MutableMap<String, IType>.mapType(t: TypeDeclaration<*>) =
    mapType(t.fullyQualifiedName.getOrNull() ?: t.nameAsString)

internal fun isJavaClassName(qualifiedName: String): Boolean = runCatching { getClassByName(qualifiedName) }.isSuccess

internal fun getTypeByName(qualifiedName: String, types: Map<String, IType> = defaultTypes): IType {
    val arrayTypeDepth = Regex("\\[\\]").findAll(qualifiedName).count()
    if (arrayTypeDepth == 0)
        return defaultTypes[qualifiedName] ?: types[qualifiedName] ?: HostRecordType(getClassByName(qualifiedName).canonicalName)

    return runCatching {
        val componentTypeName = qualifiedName.replace("[]", "")
        var type = defaultTypes[componentTypeName] ?: types[componentTypeName] ?: HostRecordType(getClassByName(componentTypeName).canonicalName)
        (0 until arrayTypeDepth).forEach { _ -> type = type.array() }
        type
    }.getOrElse { error("unsupported type $qualifiedName", qualifiedName) }
}

internal fun getClassByName(qualifiedName: String): Class<*> {
    val arrayTypeDepth = Regex("\\[\\]").findAll(qualifiedName).count()
    if (arrayTypeDepth == 0)
        return runCatching { Class.forName(qualifiedName) }.getOrElse { error("unsupported class $qualifiedName", qualifiedName) }

    return runCatching {
        val componentTypeName = qualifiedName.replace("[]", "")
        var cls = Class.forName(componentTypeName)
        (0 until arrayTypeDepth).forEach { _ -> cls = cls.arrayType() }
        cls
    }.getOrElse { error("unsupported class $qualifiedName", qualifiedName) }
}

internal fun getTypeFromJavaParser(node: Node, type: Type, types: Map<String, IType>): IType =
    runCatching { getTypeByName(type.resolve().erasure().describe(), types) }.getOrNull() ?:
    runCatching { getTypeByName(type.asString(), types) }.getOrNull() ?: error("could not find IType for type $type", node)

internal fun ResolvedType.toIType(types: Map<String, IType>): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> getTypeByName(this.qualifiedName, types)
    is ResolvedArrayType -> ArrayType(componentType.toIType(types))
    is LazyType -> getTypeByName(this.erasure().describe(), types)
    is ResolvedTypeVariable -> types["java.lang.Object"]!!
    is NullType -> NULL.type
    else -> error("unsupported expression type ${this::class.qualifiedName}", this)
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
    else -> error("unsupported expression type ${this::class.qualifiedName}", this)
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
    is HostRecordType -> this.type
    INT -> Int::class.java
    DOUBLE -> Double::class.java
    BOOLEAN -> Boolean::class.java
    CHAR -> Char::class.java
    VOID -> Void::class.java
    else -> error("IType $this has no matching Java type")
}

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

internal val IRecordType.declaringType: IRecordType?
    get() = getField(OUTER_PARAM)?.type as? IRecordType ?: (getField(OUTER_PARAM)?.type as? IReferenceType)?.target as? IRecordType

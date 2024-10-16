package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.model.typesystem.LazyType
import com.github.javaparser.resolution.model.typesystem.NullType
import com.github.javaparser.resolution.types.*
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.parsing.java.OUTER_PARAM
import pt.iscte.strudel.parsing.java.defaultTypes
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayType
import pt.iscte.strudel.parsing.java.LoadingError
import pt.iscte.strudel.vm.NULL
import java.lang.reflect.Array
import java.lang.reflect.Method
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType

internal fun typeSolver(): TypeSolver =
    CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }

internal fun MutableMap<String, IType>.mapType(
    t: String,
    location: Node
): IType = this[t] ?: getTypeByName(t, location)

internal fun MutableMap<String, IType>.mapType(t: Type, location: Node) =
    mapType(kotlin.runCatching { t.resolve().erasure().simpleNameAsString }
        .getOrDefault(t.asString()), location)

internal fun MutableMap<String, IType>.mapType(
    t: TypeDeclaration<*>,
    location: Node
) =
    mapType(t.fullyQualifiedName.getOrNull() ?: t.nameAsString, location)

internal fun isJavaClassName(qualifiedName: String, location: Node): Boolean =
    runCatching { getClassByName(qualifiedName, location) }.isSuccess

internal fun getTypeByName(
    qualifiedName: String,
    location: Node,
    types: Map<String, IType> = defaultTypes
): IType {
    val arrayTypeDepth = Regex("\\[\\]").findAll(qualifiedName).count()
    if (arrayTypeDepth == 0)
        return defaultTypes[qualifiedName] ?: types[qualifiedName]
        ?: HostRecordType(getClassByName(qualifiedName, location).canonicalName)

    return runCatching {
        val componentTypeName = qualifiedName.replace("[]", "")
        var type = defaultTypes[componentTypeName] ?: types[componentTypeName]
        ?: HostRecordType(
            getClassByName(
                componentTypeName,
                location
            ).canonicalName
        )
        (0 until arrayTypeDepth).forEach { _ -> type = type.array() }
        type.reference()
    }.getOrElse {
        LoadingError.translation(
            "could not find IType $qualifiedName",
            location
        )
    }
}

internal fun getClassByName(qualifiedName: String, location: Node): Class<*> {
    val arrayTypeDepth = Regex("\\[\\]").findAll(qualifiedName).count()
    if (arrayTypeDepth == 0)
        return runCatching { Class.forName(qualifiedName) }.getOrElse {
            LoadingError.translation(
                "could not find class $qualifiedName",
                location
            )
        }

    return runCatching {
        val componentTypeName = qualifiedName.replace("[]", "")
        var cls = Class.forName(componentTypeName)
        (0 until arrayTypeDepth).forEach { _ -> cls = cls.arrayType() }
        cls
    }.getOrElse {
        LoadingError.translation(
            "could not find class $qualifiedName",
            location
        )
    }
}

internal fun getTypeFromJavaParser(
    node: Node,
    type: Type,
    types: Map<String, IType>,
    location: Node
): IType =
    runCatching {
        getTypeByName(
            type.resolve().erasure().describe(),
            location,
            types
        )
    }.getOrNull() ?: runCatching {
        getTypeByName(
            type.asString(),
            location,
            types
        )
    }.getOrNull() ?: LoadingError.translation(
        "could not find IType $type",
        node
    )

internal fun ResolvedType.toIType(
    types: Map<String, IType>,
    location: Node
): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> getTypeByName(
        this.qualifiedName,
        location,
        types
    )

    is ResolvedArrayType -> ArrayType(componentType.toIType(types, location))
    is LazyType -> getTypeByName(this.erasure().describe(), location, types)
    is ResolvedTypeVariable -> types["java.lang.Object"]!!
    is NullType -> NULL.type
    else -> LoadingError.translation(
        "could not convert resolved type ${this::class.qualifiedName} to IType",
        location
    )
}

internal fun ResolvedType.toJavaType(location: Node): Class<*> = when (this) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClassByName(this.qualifiedName, location)
    is ResolvedArrayType -> Array.newInstance(
        this.componentType.toJavaType(
            location
        ), 0
    ).javaClass

    is LazyType -> getClassByName(this.describe(), location)
    is ResolvedTypeVariable -> Any::class.java // Generics compile to Object
    is NullType -> Nothing::class.java
    else -> LoadingError.translation(
        "could not convert resolved type ${this::class.qualifiedName} to Java class",
        location
    )
}

internal val ResolvedType.simpleNameAsString: String
    get() = when (this) {
        is ResolvedReferenceType -> this.qualifiedName
        is ResolvedArrayType -> this.componentType.simpleNameAsString + "[]"
        is ResolvedTypeVariable -> this.qualifiedName()
        else -> describe()
    }

internal fun Expression.getResolvedJavaType(): Class<*> = kotlin.runCatching {
    calculateResolvedType().toJavaType(this)
}.onFailure { println("Failed to resolve Java type of $this: $it") }
    .getOrThrow()

internal fun Expression.getResolvedIType(types: Map<String, IType>): IType =
    kotlin.runCatching {
        try {
            calculateResolvedType().toIType(types, this)
        } catch (e: UnsolvedSymbolException) {
            this.resolveHack(types) ?: throw e
        }
    }.onFailure {
        println("Failed to resolve type of $this: $it")
    }.getOrThrow()

// TODO hack to cover JP solver bug
private fun Expression.resolveHack(types: Map<String, IType>): IType? {
    if (this !is NameExpr)
        return null

    var parentFor = this.findAncestor(ForStmt::class.java)
    while (parentFor.isPresent) {

        val dec =
            parentFor.get().initialization.find {
                it is VariableDeclarationExpr &&
                        it.variables.any {
                            it.nameAsString == this.nameAsString
                        }
            } as? VariableDeclarationExpr

        dec?.let {
            types[it.commonType.asString()]?.let {
                return it
            }
        }
        parentFor = parentFor.get().findAncestor(ForStmt::class.java)
    }

    return null
}

internal fun Class<*>.findCompatibleMethod(
    name: String,
    parameterTypes: Collection<Class<*>>
): Method? =
    methods.find {
        it.name == name && it.parameterTypes.size == parameterTypes.size && it.parameterTypes.zip(
            parameterTypes
        ).all { p ->
            p.first == p.second || p.second.isAssignableFrom(p.first) || p.second.isImplicitlyCastableTo(
                p.first
            )
        }
    }

// Can this class be implicitly (widening conversion) cast to the other?
// See also: https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html
internal fun Class<*>.isImplicitlyCastableTo(other: Class<*>): Boolean =
    if (this == other) true
    else if (isPrimitive) when (this) {
        Int::class.java -> other == Long::class.java || other == Float::class.java || other == Double::class.java
        Long::class.java -> other == Float::class.java || other == Double::class.java
        Float::class.java -> other == Double::class.java
        Char::class.java -> other == Int::class.java || other == Long::class.java || other == Float::class.java || other == Double::class.java
        Byte::class.java -> other == Short::class.java || other == Int::class.java || other == Long::class.java || other == Float::class.java || other == Double::class.java
        else -> false
    }
    else other == this.wrapperType || other.isAssignableFrom(this)

internal fun IType.toJavaType(location: Node): Class<*> = when (this) {
    is IReferenceType -> target.toJavaType(location)
    is IArrayType -> Array.newInstance(
        this.componentType.toJavaType(location),
        0
    ).javaClass

    is HostRecordType -> this.type
    INT -> Int::class.java
    DOUBLE -> Double::class.java
    BOOLEAN -> Boolean::class.java
    CHAR -> Char::class.java
    VOID -> Void::class.java
    else -> LoadingError.translation(
        "IType $this has no matching Java type",
        location
    )
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
    get() = getField(OUTER_PARAM)?.type as? IRecordType
        ?: (getField(OUTER_PARAM)?.type as? IReferenceType)?.target as? IRecordType

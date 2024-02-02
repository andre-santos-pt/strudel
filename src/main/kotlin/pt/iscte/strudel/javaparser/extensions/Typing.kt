package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.resolution.model.typesystem.LazyType
import com.github.javaparser.resolution.types.ResolvedArrayType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.javaparser.defaultTypes
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.ArrayType
import java.lang.reflect.Array
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.full.companionObject

fun typeSolver(): TypeSolver {
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }
}

fun MutableMap<String, IType>.mapType(t: String): IType = this[t] ?: getTypeByName(t, this)

fun MutableMap<String, IType>.mapType(t: Type) = mapType(t.resolve().erasure().describe())

fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) =
    mapType(t.fullyQualifiedName.getOrNull() ?: t.nameAsString)

fun isJavaClassName(name: String): Boolean = runCatching { getClassByName(name) }.isSuccess

fun getTypeByName(qualifiedName: String, types: Map<String, IType> = mapOf()): IType =
    defaultTypes[qualifiedName] ?: types[qualifiedName] ?: JavaType(getClassByName(qualifiedName))
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

fun getClassByName(qualifiedName: String): Class<*> {
    val arrayTypeRegex = Regex("\\[\\]")
    val arrayTypeDepth = arrayTypeRegex.findAll(qualifiedName).count()

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
    /*
    try {
        Class.forName(name)
    } catch (e1: Exception) {
        try {
            Class.forName("java.lang.$name")
        } catch (e2: Exception) {
            try {
                Class.forName("java.util.$name")
            } catch (e3: Exception) {
                pt.iscte.strudel.javaparser.error("could not find class with name $name", name)
            }
        }
    }
     */

fun getTypeFromJavaParser(node: Node, type: Type, types: Map<String, IType>): IType =
    runCatching { getTypeByName(type.asString(), types) }.getOrNull() ?:
    runCatching { getTypeByName(type.resolve().describe(), types) }.getOrNull() ?:
    pt.iscte.strudel.javaparser.error("could not find IType for type $type", node)
    // types[type.asString()] ?: pt.iscte.strudel.javaparser.error("could not find IType for type $type", node)

internal fun ResolvedType.toIType(types: Map<String, IType>): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> types[this.qualifiedName] ?: HostRecordType(this.qualifiedName)
    is ResolvedArrayType -> ArrayType(componentType.toIType(types))
    is LazyType -> getTypeByName(this.describe(), types)
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type ${this::class.qualifiedName}", this)
}

internal fun ResolvedType.toJavaType(): Class<*> = when (this) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClassByName(this.qualifiedName)
    is ResolvedArrayType -> Array.newInstance(this.componentType.toJavaType(), 0).javaClass // TODO: test
    is LazyType -> getClassByName(this.describe())
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type ${this::class.qualifiedName}", this)
}

internal fun Expression.getResolvedJavaType(): Class<*> = calculateResolvedType().toJavaType()

internal fun Expression.getResolvedIType(types: Map<String, IType>): IType = calculateResolvedType().toIType(types)
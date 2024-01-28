package pt.iscte.strudel.javaparser.extensions

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.type.ClassOrInterfaceType
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

fun typeSolver(): TypeSolver {
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
    // combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return CombinedTypeSolver().apply { add(ReflectionTypeSolver()) }
}

fun MutableMap<String, IType>.mapType(t: String): IType =
    if (containsKey(t))
        this[t]!!
    else getType(t, this)

fun MutableMap<String, IType>.mapType(t: Type) =
    if (t is ClassOrInterfaceType)
        mapType(t.nameAsString)
    else
        mapType(t.asString())

fun MutableMap<String, IType>.mapType(t: ClassOrInterfaceDeclaration) =
    mapType(t.nameAsString)

fun isJavaClassName(name: String): Boolean = runCatching { getClass(name) }.isSuccess

fun getType(name: String, types: Map<String, IType> = mapOf()): IType = defaultTypes[name] ?: types[name] ?:
    try {
        JavaType(Class.forName(name))
    } catch (e1: Exception) {
        try {
            JavaType(Class.forName("java.lang.$name"))
        } catch (e2: Exception) {
            try {
                JavaType(Class.forName("java.util.$name"))
            } catch (e3: Exception) {
                pt.iscte.strudel.javaparser.error("unsupported type $name", name)
            }
        }
    }

fun getClass(name: String): Class<*> {
    runCatching { Class.forName(name) }.onSuccess { return it }
    Package.getPackages().forEach {
        runCatching { Class.forName("${it.name}.$name") }.onSuccess { return it }
    }
    pt.iscte.strudel.javaparser.error("unsupported type $name", name)
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
            pt.iscte.strudel.javaparser.error("unsupported type $name", name)
        }
    }
}
 */

internal fun ResolvedType.toIType(types: Map<String, IType>): IType = when (this) {
    ResolvedPrimitiveType.CHAR -> CHAR
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> INT
    ResolvedPrimitiveType.BOOLEAN -> BOOLEAN
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> DOUBLE
    is ResolvedReferenceType -> types[this.qualifiedName] ?: HostRecordType(this.qualifiedName)
    is ResolvedArrayType -> ArrayType(componentType.toIType(types))
    is LazyType -> getType(this.describe(), types) // TODO: using this.describe() seems hacky, but this.concrete is private
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type $this", this)
}

internal fun ResolvedType.toJavaType(): Class<*> = when (this) {
    ResolvedPrimitiveType.CHAR -> Char::class.java
    ResolvedPrimitiveType.INT, ResolvedPrimitiveType.LONG -> Int::class.java
    ResolvedPrimitiveType.BOOLEAN -> Boolean::class.java
    ResolvedPrimitiveType.DOUBLE, ResolvedPrimitiveType.FLOAT -> Double::class.java
    is ResolvedReferenceType -> getClass(this.qualifiedName)
    is ResolvedArrayType -> Array.newInstance(this.componentType.toJavaType(), 0).javaClass // TODO: test
    else -> pt.iscte.strudel.javaparser.error("unsupported expression type $this", this)
}

internal fun Expression.getResolvedJavaType(): Class<*> = calculateResolvedType().toJavaType()

internal fun Expression.getResolvedIType(types: Map<String, IType>): IType = calculateResolvedType().toIType(types)
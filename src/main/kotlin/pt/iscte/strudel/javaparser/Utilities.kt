package pt.iscte.strudel.javaparser

import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.JavaType
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

fun MutableMap<String, IType>.mapType(t: String): IType =
    if (containsKey(t))
        this[t]!!
    else {
        try {
            JavaType(Class.forName(t))
        } catch (e1: Exception) {
            try {
                JavaType(Class.forName("java.lang.$t"))
            } catch (e2: Exception) {
                try {
                    JavaType(Class.forName("java.util.$t"))
                } catch (e3: Exception) {
                    error("type", t)
                }
            }
        }
    }

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
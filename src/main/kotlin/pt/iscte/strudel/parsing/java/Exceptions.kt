package pt.iscte.strudel.parsing.java

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import pt.iscte.strudel.parsing.java.extensions.nodeAtLine
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

enum class LoadingErrorType {
    JAVA_COMPILATION,
    TRANSLATION,
    UNSUPPORTED
}

data class CompilationError(
    val type: String,
    val message: String,
    val location: SourceLocation,
    val node: Node? = null
)

class LoadingError(
    val type: LoadingErrorType,
    val messages: List<CompilationError>
) : RuntimeException(messages.joinToString("\n") { it.message }) {

    constructor(type: LoadingErrorType, message: String, location: SourceLocation, node: Node? = null) :
        this(type, listOf(CompilationError(
            LoadingErrorType.JAVA_COMPILATION.name,
            message,
            location, node)
        ))

    @Suppress("NOTHING_TO_INLINE")
    companion object {

        // COMPILATION

        inline fun compilation(
            diagnostics: List<Diagnostic<out JavaFileObject>>,
            source: String? = null
        ): Nothing {
            throw LoadingError(LoadingErrorType.JAVA_COMPILATION, diagnostics.map {
                CompilationError(
                    it.code,
                    (it.getMessage(null).lines().firstOrNull() ?: it.getMessage(Locale.getDefault())),
                    SourceLocation(it),
                    runCatching {
                        source?.let { src -> StaticJavaParser.parse(src).nodeAtLine(it.lineNumber.toInt()) }
                    }.getOrNull()
                )
            })
        }

        // TRANSLATION

        inline fun translation(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.TRANSLATION, message, SourceLocation(node))

        // UNSUPPORTED

        inline fun unsupported(messages: List<Pair<String, SourceLocation>>): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, messages.map {
                CompilationError(
                    LoadingErrorType.JAVA_COMPILATION.toString(),
                    "unsupported ${it.first}",
                    it.second
                ) })

        inline fun unsupported(message: String, location: SourceLocation): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, "unsupported $message", location)

        inline fun unsupported(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, "unsupported $message", SourceLocation(node))
    }
}

enum class ReturnError {
    EXCEPTION_THROWN,
    ASSERTION_FAILED
}
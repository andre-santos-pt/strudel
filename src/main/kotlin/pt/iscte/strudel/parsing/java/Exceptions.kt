package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

enum class LoadingErrorType {
    JAVA_COMPILATION,
    TRANSLATION,
    UNSUPPORTED
}

data class CompilationError(
    val message: String,
    val location: SourceLocation
)

class LoadingError(val type: LoadingErrorType, val messages: List<CompilationError>) :
    RuntimeException(messages.joinToString("; ") { it.message }) {

    constructor(type: LoadingErrorType, message: String, location: SourceLocation) :
            this(type, listOf(CompilationError(message, location)))

    @Suppress("NOTHING_TO_INLINE")
    companion object {

        // COMPILATION

        inline fun compilation(diagnostics: List<Diagnostic<out JavaFileObject>>): Nothing {
            throw LoadingError(LoadingErrorType.JAVA_COMPILATION, diagnostics.map {
                // TODO add somewhere "line ${it.lineNumber}: " +
                CompilationError((it.getMessage(null).lines().firstOrNull() ?: "") , SourceLocation(it))
            })
        }

        // TRANSLATION

        inline fun translation(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.TRANSLATION, message, SourceLocation(node))

        // UNSUPPORTED

        inline fun unsupported(messages: List<Pair<String, SourceLocation>>): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, messages.map { CompilationError("unsupported ${it.first}", it.second) })

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
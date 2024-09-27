package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

enum class LoadingErrorType {
    JAVA_COMPILATION,
    TRANSLATION,
    UNSUPPORTED
}

class LoadingError(val type: LoadingErrorType, val messages: List<Pair<String, SourceLocation>>) : RuntimeException(messages.joinToString { it.first }) {

    constructor(type: LoadingErrorType, message: String, location: SourceLocation) : this(type, listOf(Pair(message, location)))

    @Suppress("NOTHING_TO_INLINE")
    companion object {

        // COMPILATION

        inline fun compilation(diagnostics: List<Diagnostic<out JavaFileObject>>): Nothing {
            throw LoadingError(LoadingErrorType.JAVA_COMPILATION, diagnostics.map {
                Pair(it.getMessage(null), SourceLocation(it))
            })
        }

        // TRANSLATION

        inline fun translation(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.TRANSLATION, message, SourceLocation(node))

        // UNSUPPORTED

        inline fun unsupported(messages: List<Pair<String, SourceLocation>>): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, messages.map { Pair("unsupported ${it.first}", it.second) })

        inline fun unsupported(message: String, location: SourceLocation): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, "unsupported $message", location)

        inline fun unsupported(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, "unsupported $message", SourceLocation(node))
    }
}
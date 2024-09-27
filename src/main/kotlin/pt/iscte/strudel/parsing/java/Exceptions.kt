package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

enum class LoadingErrorType {
    JAVA_COMPILATION,
    TRANSLATION,
    UNSUPPORTED
}

class LoadingError(val type: LoadingErrorType, message: String, val locations: List<SourceLocation>) : RuntimeException(message) {

    constructor(type: LoadingErrorType, message: String, location: SourceLocation) : this(type, message, listOf(location))

    @Suppress("NOTHING_TO_INLINE")
    companion object {

        // COMPILATION

        inline fun compilation(diagnostics: List<Diagnostic<out JavaFileObject>>): Nothing {
            val message = diagnostics.joinToString(System.lineSeparator()) { it.getMessage(null) }
            val locations = diagnostics.map { SourceLocation(it) }
            throw LoadingError(LoadingErrorType.JAVA_COMPILATION, message, locations)
        }

        // TRANSLATION

        inline fun translation(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.TRANSLATION, message, SourceLocation(node))

        // UNSUPPORTED

        inline fun unsupported(message: String, locations: List<SourceLocation>): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, message, locations)

        inline fun unsupported(message: String, location: SourceLocation): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, message, location)

        inline fun unsupported(message: String, node: Node): Nothing =
            throw LoadingError(LoadingErrorType.UNSUPPORTED, message, SourceLocation(node))
    }
}
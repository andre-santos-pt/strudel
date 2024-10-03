package pt.iscte.strudel.parsing.java.extensions

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.net.URI
import java.nio.charset.Charset
import java.util.*
import javax.tools.*
import kotlin.collections.HashMap

import javax.tools.JavaFileObject.Kind.SOURCE as SOURCE

internal fun List<Diagnostic<out JavaFileObject>>.pretty(): String =
    joinToString(System.lineSeparator()) { "[Line ${it.lineNumber}] ${it.getMessage(null)}" }

/**
 * Helper class for using the Java Compiler API.
 */
internal object ClassLoader {

    /**
     * Does the Java source code file compile successfully?
     * @param file Source code file.
     */
    fun validate(file: File): Boolean = compile(file).isEmpty()

    /**
     * Does the Java source code compile successfully?
     * @param name Class name.
     * @param src Source code string.
     */
    fun validate(name: String, src: String): Boolean = compile(name, src).isEmpty()

    /**
     * Compile Java source code.
     * @param file Source code file.
     * @return A list of compilation errors.
     */
    fun compile(file: File): List<Diagnostic<out JavaFileObject>> =
        compile(file.nameWithoutExtension, file.readText(Charset.defaultCharset()))

    /**
     * Compile Java source code.
     * @param name Class name.
     * @param src Source code string.
     * @return A list of compilation errors.
     */
    fun compile(name: String, src: String): List<Diagnostic<out JavaFileObject>> {
        val javac = ToolProvider.getSystemJavaCompiler()
        val error = ByteArrayOutputStream()
        val diagnostics = DiagnosticCollector<JavaFileObject>()

        val standardFileManager = javac.getStandardFileManager(null, null, null)
        val fileManager = MemoryFileManager(standardFileManager)

        val task = javac.getTask(
            PrintWriter(error),
            fileManager,
            diagnostics,
            listOf("-XDuseUnsharedTable"),
            null,
            listOf(JavaSourceFromString(name, src))
        )

        task.call()
        fileManager.close()
        standardFileManager.close()

        return diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }
    }

    private class JavaSourceFromString(name: String, val code: String): SimpleJavaFileObject(
        URI.create("string:///${name.replace('.', '/')}${SOURCE.extension}"), SOURCE
    ) {
        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
    }

    private class MemoryFileManager(manager: JavaFileManager?): ForwardingJavaFileManager<JavaFileManager?>(manager) {

        private val classBytes: MutableMap<String, ByteArrayOutputStream> = HashMap()

        @Throws(IOException::class)
        override fun getJavaFileForOutput(
            location: JavaFileManager.Location,
            className: String,
            kind: JavaFileObject.Kind,
            sibling: FileObject
        ): JavaFileObject = MemoryJavaFileObject(className, kind)

        private inner class MemoryJavaFileObject(private val name: String, kind: JavaFileObject.Kind) :
            SimpleJavaFileObject(
                URI.create("string:///${name.replace("\\.".toRegex(), "/")}${kind.extension}"), kind
            ) {
                private val byteCode: ByteArrayOutputStream = ByteArrayOutputStream()

                @Throws(IOException::class)
                override fun openOutputStream(): ByteArrayOutputStream {
                    classBytes[name] = byteCode
                    return byteCode
                }

                override fun delete(): Boolean {
                    classBytes.remove(name)
                    return true
                }
        }
    }
}
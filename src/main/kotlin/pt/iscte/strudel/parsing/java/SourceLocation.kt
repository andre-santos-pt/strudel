package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

open class SourceLocation(val startLine: Int, val endLine: Int, val startColumn: Int, val endColumn: Int) {

    constructor(node: Node) : this(
        node.range.get().begin.line,
        node.range.get().end.line,
        node.range.get().begin.column,
        node.range.get().end.column,
    )

    constructor(diagnostic: Diagnostic<out JavaFileObject>) : this(
        diagnostic.lineNumber.toInt(),
        diagnostic.lineNumber.toInt(),
        diagnostic.columnNumber.toInt(),
        (diagnostic.columnNumber +  diagnostic.endPosition - diagnostic.startPosition).toInt()
    )

    override fun toString(): String =
        if (startLine == endLine) "$startLine:$startColumn-:$endColumn"
        else "$startLine:$startColumn-$endLine:$endColumn"
}
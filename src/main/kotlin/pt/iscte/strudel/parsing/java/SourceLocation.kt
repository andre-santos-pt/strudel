package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

class SourceLocation(
    val startLine: Int,
    val endLine: Int,
    val startColumn: Int,
    val endColumn: Int,
    val length: Int
) {

    constructor(node: Node) : this(
        node.range.get().begin.line,
        node.range.get().end.line,
        node.range.get().begin.column,
        node.range.get().end.column,
        if(node.range.get().begin.line == node.range.get().end.line)
            node.range.get().end.column - node.range.get().begin.column + 1
        else
            node.tokenRange.get().sumOf { it.text.length }
    )

    constructor(diagnostic: Diagnostic<out JavaFileObject>) : this(
        diagnostic.lineNumber.toInt(),
        diagnostic.lineNumber.toInt(),
        diagnostic.columnNumber.toInt(),
        (diagnostic.columnNumber +  diagnostic.endPosition - diagnostic.startPosition).toInt(),
        (diagnostic.endPosition - diagnostic.startPosition).toInt()
    )

    override fun toString(): String =
        if (startLine == endLine) "$startLine:$startColumn-$endColumn"
        else "$startLine:$startColumn-$endLine:$endColumn"
}
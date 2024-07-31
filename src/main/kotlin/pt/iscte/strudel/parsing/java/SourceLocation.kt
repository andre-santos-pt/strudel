package pt.iscte.strudel.parsing.java

import com.github.javaparser.ast.Node

// TODO add end line/column
open class SourceLocation(val line: Int, val start: Int, val end: Int) {

    constructor(node: Node) : this(node.range.get().begin.line, node.range.get().begin.column, node.range.get().end.column)

    override fun toString(): String = "Line: $line: $start-$end"
}
package pt.iscte.strudel.javaparser

import com.github.javaparser.ast.Node

open class SourceLocation(val line: Int, val start: Int, val end: Int) {

    constructor(node: Node) : this(node.range.get().begin.line, node.range.get().begin.column, node.range.get().end.column)

    override fun toString(): String {
        return "Line: $line: $start-$end"
    } //	public static SourceLocation create(ParserRuleContext ctx) {
    //		int end = ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length() - 1;
    //		SourceLocation loc = new SourceLocation(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), end);
    //		return loc;
    //	}
}

class IdLocation(line: Int, start: Int, end: Int) : SourceLocation(line, start, end)
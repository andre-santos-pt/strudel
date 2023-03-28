package pt.iscte.strudel.javaparser

open class SourceLocation(val line: Int, val start: Int, val end: Int) {
    override fun toString(): String {
        return "Line: $line - $start : $end"
    } //	public static SourceLocation create(ParserRuleContext ctx) {
    //		int end = ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length() - 1;
    //		SourceLocation loc = new SourceLocation(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), end);
    //		return loc;
    //	}
}

class IdLocation(line: Int, start: Int, end: Int) : SourceLocation(line, start, end)
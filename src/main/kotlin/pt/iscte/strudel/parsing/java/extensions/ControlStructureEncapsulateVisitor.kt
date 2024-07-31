package pt.iscte.strudel.parsing.java.extensions

import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

object ControlStructureEncapsulateVisitor : VoidVisitorAdapter<Any>() {

    override fun visit(n: IfStmt, arg: Any?) {
        if (n.thenStmt !is BlockStmt) {
            val thenStmt = n.thenStmt
            val block = BlockStmt()
            n.setThenStmt(block)
            if (thenStmt != null)
                block.addStatement(thenStmt)
        }
        if (n.hasElseBranch() && n.elseStmt.get() !is BlockStmt) {
            val elseStmt = n.elseStmt.get()
            val block = BlockStmt()
            n.setElseStmt(block)
            block.addStatement(elseStmt)
        }
        super.visit(n, arg)
    }

    override fun visit(n: WhileStmt, arg: Any?) {
        if (n.body !is BlockStmt) {
            val body = n.body
            val block = BlockStmt()
            n.setBody(block)
            if (body != null)
                block.addStatement(body)
        }
        super.visit(n, arg)
    }

    override fun visit(n: DoStmt, arg: Any?) {
        if (n.body !is BlockStmt) {
            val body = n.body
            val block = BlockStmt()
            n.setBody(block)
            if (body != null)
                block.addStatement(body)
        }
        super.visit(n, arg)
    }

    override fun visit(n: ForStmt, arg: Any?) {
        if (n.body !is BlockStmt) {
            val body = n.body
            val block = BlockStmt()
            n.setBody(block)
            if (body != null)
                block.addStatement(body)
        }
        super.visit(n, arg)
    }

    override fun visit(n: ForEachStmt, arg: Any?) {
        if (n.body !is BlockStmt) {
            val body = n.body
            val block = BlockStmt()
            n.setBody(block)
            if (body != null)
                block.addStatement(body)
        }
        super.visit(n, arg)
    }
}
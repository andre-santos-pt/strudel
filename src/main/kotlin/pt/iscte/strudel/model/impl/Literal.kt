package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.CHAR
import pt.iscte.strudel.model.ILiteral
import pt.iscte.strudel.model.IType


internal class Literal(override val type: IType, override val  stringValue: String) : Expression(), ILiteral {
    override fun toString(): String =
        when(type) {
            CHAR -> "'$stringValue'"
            else -> stringValue
        }
}

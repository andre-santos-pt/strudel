package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.roles.impl.FixedValue

interface IFixedValue : IVariableRole {
    val isModified: Boolean

    override val name: String
        get() = "Fixed Value"
}
package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableAssignment

interface IOneWayFlag : IVariableRole {
    val assignment: IVariableAssignment
    val conditions: List<IProgramElement>
    override val name: String
        get() = "OneWayflag"
}
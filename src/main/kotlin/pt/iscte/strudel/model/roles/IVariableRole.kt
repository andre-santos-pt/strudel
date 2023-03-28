package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.roles.impl.*

interface IVariableRole {
    val name: String

    companion object {
        fun match(v: IVariableDeclaration<*>): IVariableRole {
            return when {
                FixedValue.isFixedValue(v) -> FixedValue(v)
                Gatherer.isGatherer(v)-> Gatherer(v)
                ArrayIndexIterator.isArrayIndexIterator(v) -> ArrayIndexIterator(v) // before Stepper, because its a special case of it
                Stepper.isStepper(v) -> Stepper(v)
                MostWantedHolder.isMostWantedHolder(v) -> MostWantedHolder(v)
                OneWayFlag.isOneWayFlag(v) -> OneWayFlag(v)
                else -> NONE
            }
        }

        val NONE: IVariableRole = object : IVariableRole {
            override val name: String
                get() = "no role"

            override fun toString(): String {
                return name
            }
        }
    }
}
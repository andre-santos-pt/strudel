package pt.iscte.strudel.model.roles.impl

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.roles.IArrayIndexIterator
import pt.iscte.strudel.model.roles.IStepper

fun <K,V> MutableMap<K,MutableList<V>>.putMulti(key: K, value: V) {
    if (containsKey(key))
        get(key)!!.add(value)
    else
        put(key, mutableListOf(value))
}

class ArrayIndexIterator(variable: IVariableDeclaration<*>) : IArrayIndexIterator {
    override val direction: IStepper.Direction?
    override val arrayVariables: List<IVariableDeclaration<*>>
    override val stepSize: Int
    override val arrayExpressionsMap: Map<IVariableDeclaration<*>, List<IProgramElement>>
    override val expressions: List<IProgramElement>
    override val initializationValue: IExpression?
    override val cycleLimit: IExpression?
        get() = TODO("Not yet implemented")
    private val upperLimit: IExpression?
    override val isIteratingWholeArray: Boolean

    init {
        require(isArrayIndexIterator(variable))
        val stepper = Stepper(variable)
        val v = Visitor(variable)
        variable.ownerProcedure.accept(v)
        direction = stepper.direction
        stepSize = stepper.stepSize
        initializationValue = stepper.initializationValue
        upperLimit = stepper.cycleLimit
        expressions = stepper.expressions
        arrayVariables = v.arrayVariables
        arrayExpressionsMap = v.arrayExpressionsMap
        isIteratingWholeArray = stepper.isIteratingWholeArray
    }


    override fun toString(): String {
        return "$name($direction, $arrayVariables)"
    }

    private class Visitor(val variable: IVariableDeclaration<*>) : IBlock.IVisitor {
        var arrayVariables = mutableListOf<IVariableDeclaration<*>>()
        var arrayExpressionsMap = mutableMapOf<IVariableDeclaration<*>, MutableList<IProgramElement>>()

        override fun visit(arrayElement: IArrayAccess): Boolean {    //... = v[i] + ....
            val arrayVariable: IVariableDeclaration<*> = (arrayElement.target as IVariableExpression).variable
            if (arrayElement.index.includes(variable)) {
                arrayExpressionsMap.putMulti(arrayVariable, arrayElement)
                if (!arrayVariables.contains(arrayVariable)) arrayVariables.add(arrayVariable)
            }
            return false
        }

        private fun getMultidimensionalArrayAccessVariable(access: IArrayAccess): IVariableExpression? =
            when (val target = access.target) {
                is IVariableExpression -> target
                is IArrayAccess -> getMultidimensionalArrayAccessVariable(target)
                else -> null
            }

        override fun visit(assignment: IArrayElementAssignment): Boolean {    //v[i] = ...
                val arrayVariable: IVariableDeclaration<*>? =
                    getMultidimensionalArrayAccessVariable(assignment.arrayAccess)?.variable

                if (arrayVariable == null)
                    throw NoSuchElementException("Cannot find array variable for: $assignment")

                if (assignment.arrayAccess.index.includes(variable)) {
                    arrayExpressionsMap.putMulti(arrayVariable, assignment)
                    if (!arrayVariables.contains(arrayVariable)) arrayVariables.add(arrayVariable)
                }
            return true
        }

        val isUsedInArrays: Boolean
            get() = arrayVariables.size > 0
    }

    companion object {
        fun isArrayIndexIterator(variable: IVariableDeclaration<*>): Boolean {
            if (!Stepper.isStepper(variable)) return false
            val v = Visitor(variable)
            variable.ownerProcedure.accept(v)
            return v.isUsedInArrays
        }
    }
}
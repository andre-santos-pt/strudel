package pt.iscte.strudel.model.roles

import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IVariableDeclaration

interface IArrayIndexIterator : IStepper {
    //arrays em que a variavel é usada
    val arrayVariables: List<Any?>?

    /**
     * Note: map.keys() is the same as getArrayVarriables().
     * Useful for expression marking on Javardise.
     * @return Returns a Multimap with sets of IVariableDeclarations and IProgramElements,
     * Each different IVariableDeclaration represent an array and the IProgramElements are the expressions
     * which support the iterated array claims. (IArrayAssignments and IArrayElements)
     */
    val arrayExpressionsMap: Map<IVariableDeclaration<*>, List<IProgramElement>>
    override val name: String
        get() = "Array Index Iterator"
}
package pt.iscte.strudel.vm

import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableDeclaration


interface IExecutionData {
    val assignmentData: Map<IProcedure, Int>
    val totalAssignments: Int
    val totalProcedureCalls: Int
    val callStackDepth: Int
    val returnValue: IValue
    fun getVariableValue(id: IVariableDeclaration<IBlock>): IValue

    fun printResult() {
        println(returnValue)
    }
}
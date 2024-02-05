package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.dsl.Translator

data class SemanticProblem(val element: IProgramElement, val message: String) {
    override fun toString() = Translator.translate(element) + ": " + message
}

fun IModule.checkSemantics() :  List<SemanticProblem> {
    var invalid = mutableListOf<SemanticProblem>()
    procedures.filterIsInstance<IProcedure>().forEach {
       invalid.addAll(it.checkSemantics())
    }
    return invalid
}

fun IProcedure.checkSemantics(): List<SemanticProblem> {
    var invalid = mutableListOf<SemanticProblem>()

    val check = object : IBlock.IVisitor {
        override fun visit(exp: IVariableExpression) {
            if (!variables.contains(exp.variable))
                invalid.add(SemanticProblem(exp.variable, "variable is not defined in the procedure"))
        }

        override fun visit(ret: IReturn): Boolean {
            if (returnType.isVoid) {
                if (!ret.isVoid)
                    invalid.add(SemanticProblem(ret, "procedure does not return anything (void)"))
            }
            else if(!returnType.isUnbound) {
                if (ret.expression == null) {
                    invalid.add(SemanticProblem(ret, "procedure should return a value"))
                } else if (!returnType.isSame(ret.expression!!.type)) {
                    invalid.add(SemanticProblem(ret, "returned value has to be of type ${returnType.id}"))
                }
            }
            return true
        }

        override fun visit(assignment: IVariableAssignment): Boolean {
            if(!assignment.target.type.isSame(assignment.expression.type))
                invalid.add(SemanticProblem(assignment.expression,
                    "expression type (${Translator.translate(assignment.expression)}) does not match variable type (${Translator.translate(assignment.target.type)})"))
            return true
        }

        override fun visit(assignment: IArrayElementAssignment): Boolean {
            if(!assignment.expression.type.isSame(assignment.arrayAccess.type))
                invalid.add(SemanticProblem(assignment.expression,
                    "expression type (${Translator.translate(assignment.expression)}) does not match array element type (${Translator.translate(assignment.arrayAccess.type)})"))

//            val t = (assignment.arrayAccess.target.type as IReferenceType).target as IArrayType
//            if(assignment.arrayAccess.indexes.size != t.dimensions)
//                invalid.add(SemanticProblem(assignment, "number of indexes must match array dimensions"))

            if(assignment.arrayAccess.index.type != INT)
                invalid.add(SemanticProblem(assignment.arrayAccess.index,"index expression type (${assignment.index} - ${assignment.arrayAccess.index.type}"))
            return true
        }

        override fun visit(call: IProcedureCall): Boolean {
            if(call.procedure.parameters.size != call.arguments.size)
                invalid.add(SemanticProblem(call,
                    "${call.procedure.id} requires ${call.procedure.parameters.size} arguments (instead of ${call.arguments.size})"))
            else {
                call.procedure.parameters.forEachIndexed { i, p ->
                    if (!p.type.isSame(call.arguments[i].type))
                        invalid.add(SemanticProblem(call.arguments[i], "argument has to be of type ${p.type.id}"))
                }
            }
            return true
        }

        override fun visit(exp: IProcedureCallExpression): Boolean {
            return visit(exp as IProcedureCall)
        }

        override fun visit(assignment: IRecordFieldAssignment): Boolean {
            return super.visit(assignment)
        }

        override fun visit(selection: ISelection): Boolean {
            checkGuard(selection)
            return true
        }

        override fun visit(loop: ILoop): Boolean {
            checkGuard(loop)
            return true
        }

        private fun checkGuard(structure: IControlStructure) {
            if (structure.guard.type != BOOLEAN)
                invalid.add(
                    SemanticProblem(
                        structure.guard,
                        "guard must be a boolean expression (instead of ${structure.guard.type.id})"
                    )
                )
        }

        override fun visit(exp: IArrayAllocation): Boolean {
            exp.dimensions.checkAllIntType()
            return true
        }

        override fun visit(breakStatement: IBreak) {

        }

        override fun visit(continueStatement: IContinue) {

        }



        fun List<IExpression>.checkAllIntType() {
            forEach {
                if(it.type != pt.iscte.strudel.model.INT)
                    invalid.add(SemanticProblem(it,
                        "expression for array dimension must be of integer type (instead of ${it.type})"))
            }
        }
    }
    block.accept(check)

    // var scope

    // dead code

    // return reachability

    return invalid
}


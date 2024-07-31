package pt.iscte.strudel.model

import pt.iscte.strudel.parsing.java.THIS_PARAM


typealias IParameter = IVariableDeclaration<IProcedureDeclaration>

interface IProcedureDeclaration : IModuleMember {
    val module: IModule?
    val parameters: List<IParameter>
    val returnType: IType
    fun addParameter(type: IType): IParameter
    fun expression(args: List<IExpression>): IProcedureCallExpression

    fun expression(vararg args: IExpression): IProcedureCallExpression {
        return expression(listOf(*args))
    }

    fun shortSignature(): String {
        return "$id(...)"
    }

    fun longSignature(): String {
        var args = ""
        for (p in parameters) {
            if (args.isNotEmpty()) args += ", "
            args += p.type
        }
        return "$returnType $id($args)"
    }

//    fun matchesSignature(id: String, args: List<IExpression>): Boolean {
//        return matchesSignature(id, *args.map { it.type })
//    }

    val thisParameter: IParameter get() = parameters.find { it.id == THIS_PARAM }!!

    /*
    fun matchesSignature(id: String, vararg paramTypes: IType): Boolean {
        if (id != this.id) return false
        val parameters = parameters
        if (parameters.size != paramTypes.size) return false
        var i = 0
        for (t in paramTypes) if (!parameters[i++].type.isSame(t)) return false
        return true
    }
     */

    // compares id and types of parameters
    // excludes return
    fun hasSameSignature(procedure: IProcedureDeclaration): Boolean {
        if (id != procedure.id || parameters.size != procedure.parameters.size) return false
        val procParamsIt = procedure.parameters.iterator()
        for (p in parameters) if (p.type != procParamsIt.next().type) return false
        return true
    }

    fun isEqualTo(procedure: IProcedureDeclaration): Boolean {
        return hasSameSignature(procedure) && returnType == procedure.returnType
    }
}


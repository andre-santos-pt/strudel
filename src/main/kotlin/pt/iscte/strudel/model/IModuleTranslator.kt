package pt.iscte.strudel.model

interface IModuleTranslator {
    fun translate(module: IModuleView): String
    fun translate(element: IBlockElement): String
    fun translate(expression: IExpression): String
    fun translate(type: IType): String
    fun translate(procedure: IProcedure): String
    fun translate(element: IProgramElement): String =
        when(element) {
            is IBlockElement -> translate(element)
            is IExpression -> translate(element)
            is IType -> translate(element)
            is IProcedure -> translate(element)
            else -> element.toString()
        }
}
package pt.iscte.strudel.model.dsl

import pt.iscte.strudel.model.*
import java.util.*

object Translator : IModuleTranslator {
    private val translator: IModuleTranslator?

    init {
        val t: Optional<IModuleTranslator> = ServiceLoader.load(IModuleTranslator::class.java).findFirst()
        translator = if (t.isPresent) t.get() else null
    }


     override fun translate(module: IModuleView): String {
        return translator?.translate(module) ?: defaultString(module)
    }

    override fun translate(element: IBlockElement): String {
        return translator?.translate(element) ?: defaultString(element)
    }

    override fun translate(expression: IExpression): String {
        return translator?.translate(expression) ?: defaultString(expression)
    }

    override fun translate(type: IType): String {
        return translator?.translate(type) ?: defaultString(type)
    }

    override fun translate(procedure: IProcedure): String {
        return translator?.translate(procedure) ?: defaultString(procedure)
    }

    private fun defaultString(e: IProgramElement): String {
        return e.id ?: e.javaClass.simpleName.toString() //+ ":" + e
    }
}
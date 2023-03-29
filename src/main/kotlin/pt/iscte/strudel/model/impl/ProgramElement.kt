package pt.iscte.strudel.model.impl

import pt.iscte.strudel.model.IBlockHolder
import pt.iscte.strudel.model.IProgramElement

internal open class ProgramElement(vararg flags: String) : IProgramElement {
    private var properties: Map<String, Any> = emptyMap()

    init {
        for (f in flags) setFlag(f)
    }

    override fun getProperty(key: String): Any? {
        return properties[key]
    }

    override fun setProperty(key: String, value: Any?) {
        if(value == null) {
            if (properties.isNotEmpty())
                (properties as MutableMap).remove(key)
        }
        else {
            if (properties.isEmpty())
                properties = mutableMapOf()

            (properties as MutableMap)[key] = value
        }
    }

    override val flags: List<String>
        get() = properties.filterValues { it == true }.keys.toList()

    override fun cloneProperties(e: IProgramElement) {
        (e as ProgramElement).properties.forEach {
            setProperty(it.key, it.value)
        }
    }

    internal fun tabs(b: IBlockHolder): String {
        var tabs = ""
        val d = b.block.depth
        for (i in 0 .. d) tabs += "\t"
        return tabs
    }
}
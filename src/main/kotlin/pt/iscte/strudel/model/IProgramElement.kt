package pt.iscte.strudel.model

const val ID_PROP = "ID"
const val NAMESPACE_PROP = "NAMESPACE"
const val DOC_PROP = "DOC"

interface IProgramElement {
    fun setProperty(key: String, value: Any?)
    fun getProperty(key: String): Any?

    fun cloneProperties(e: IProgramElement)

    fun <T> getProperty(keyClass: Class<T>) : T? = getProperty(keyClass.simpleName) as? T

    fun hasProperty(key: String): Boolean = getProperty(key) != null

    fun isSame(e: IProgramElement): Boolean {
        return this === e
    }

    fun setFlag(vararg keys: String) {
        for (k in keys) setProperty(k, true)
    }

    fun unsetFlag(key: String) {
        setProperty(key, false)
    }

    fun setProperty(value: Any) {
        setProperty(value.javaClass.simpleName, value)
    }

    fun hasFlag(key: String): Boolean {
        return getProperty(key) == true
    }

    fun not(key: String): Boolean {
        return getProperty(key) != true
    }

    var id: String?
        get() = getProperty(ID_PROP) as? String
        set(value) = setProperty(ID_PROP, value)

    var documentation: String?
        get() = getProperty(DOC_PROP) as? String
        set(value) = setProperty(DOC_PROP, value)

    val flags: List<String>
        get() = emptyList()
}

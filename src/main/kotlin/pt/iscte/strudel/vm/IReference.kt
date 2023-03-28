package pt.iscte.strudel.vm


interface IReference<T:IValue> : IValue {
    val target: T
    override fun copy(): IReference<T>
    override val isNull
        get() = target as IValue === NULL
}
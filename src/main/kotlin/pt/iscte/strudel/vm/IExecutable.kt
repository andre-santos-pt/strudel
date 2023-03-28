package pt.iscte.strudel.vm


interface IExecutable {
    //@Throws(RuntimeError::class)
    fun execute(stack: ICallStack, expressions: List<IValue>)
}
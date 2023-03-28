package pt.iscte.strudel.vm

interface IEvaluable {
    //@Throws(RuntimeError::class)
    fun evalutate(values: List<IValue>, stack: ICallStack): IValue
}
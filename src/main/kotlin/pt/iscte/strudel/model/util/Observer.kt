package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*


open class Observable<E> {
    private val observers = mutableListOf<(E) -> Unit>()
    fun addListener(reaction: (E) -> Unit) = observers.add(reaction)
    fun removeListener(reaction: (E) -> Unit) = observers.remove(reaction)
    fun notifyObservers(event: E) = observers.forEach { it(event) }
}

enum class EventType {
    ADD, REMOVE, MOVE, MODIFY
}

class PropertyEvent(val id: Any, val newValue: Any?, val oldValue: Any?)

open class ModuleListener(val module: IModule) :
    IModule by module, Observable<ModuleListener.Event>() {

    class Event(val type: EventType, val element: IModuleMember)

    override fun add(member: IModuleMember) {
        module.add(member)
        notifyObservers(Event(EventType.ADD, member))
    }
}


open class BlockListener(val target: IBlock) :
    IBlock by target, Observable<BlockListener.Event>() {

    class Event(val type: EventType, val element: IBlockElement, val index: Int = -1, val oldIndex: Int = -1)

    open override fun add(element: IBlockElement, index: Int) {
        val a = target.add(element, index)
        notifyObservers(Event(EventType.ADD, element, index))
        return a
    }

    override fun remove(element: IBlockElement) {
        target.remove(element)
        notifyObservers(Event(EventType.REMOVE, element))
    }

    override fun moveTo(element: IBlockElement, index: Int) {
        val old = this.block.indexOf(element)
        this.target.moveTo(element, index)
        notifyObservers(Event(EventType.MOVE, element, index, old))
    }
}


class ProcedureListener(val procedure: IProcedure) :
    IProcedure by procedure, Observable<PropertyEvent>() {

    override var id
        get() = procedure.id
        set(value) {
            notifyObservers(PropertyEvent("ID", value, this.id))
            this.id = value
        }

    override fun setProperty(key: String, value: Any?) {
        val old = procedure.getProperty(key)
        procedure.setProperty(key, value)
        notifyObservers(PropertyEvent(key, value, old))
    }
}


class PropertyListener(val element: IProgramElement) :
    IProgramElement by element, Observable<PropertyEvent>() {

    override fun setProperty(key: String, value: Any?) {
        val old = element.getProperty(key)
        element.setProperty(key, value)
        notifyObservers(PropertyEvent(key, value, old))
    }
}

class AssignmentListener(val assignment: IVariableAssignment) :
    IVariableAssignment by assignment, Observable<PropertyEvent>() {

    override var target: IVariableDeclaration<*>
        get() = assignment.target
        set(value) {
            val old = assignment.target
            assignment.target = value
            notifyObservers(PropertyEvent("VARIABLE", value, old))
        }

    override var expression: IExpression
        get() = assignment.expression
        set(value) {
            val old = assignment.expression
            assignment.expression = value
            notifyObservers(PropertyEvent("EXPRESSION", value, old))
        }
}

class ControlStructureListener(val control: IControlStructure) :
    IControlStructure by control, Observable<PropertyEvent>() {
    override var guard: IExpression
        get() = control.guard
        set(value) {
            val old = control.guard
            control.guard = value
            notifyObservers(PropertyEvent("GUARD", value, old))
        }
}

class ReturnListener(val ret: IReturn) :
    IReturn by ret, Observable<PropertyEvent>() {
    override var expression: IExpression?
        get() = ret.expression
        set(value) {
            val old = ret.expression
            ret.expression = value
            notifyObservers(PropertyEvent("EXPRESSION", value, old))
        }
}

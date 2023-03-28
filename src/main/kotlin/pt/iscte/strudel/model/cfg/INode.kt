package pt.iscte.strudel.model.cfg

import pt.iscte.strudel.model.IProgramElement

interface INode {
    var next: INode? // sequence or false
    val element: IProgramElement?
    val isEntry: Boolean
    val isExit: Boolean
    val incoming: Set<INode>
    fun isEquivalentTo(node: INode): Boolean //	default boolean isEntry() {
    //		return getElement() == null && getIncomming().isEmpty();
    //	}
    //	
    //	default boolean isExit() {
    //		return getElement() == null && getNext() == null;
    //	}
}
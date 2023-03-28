package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IBlockElement
import pt.iscte.strudel.model.IBlockHolder
import kotlin.reflect.KClass
import kotlin.reflect.cast

//inline fun <reified T : IBlockElement> IBlockHolder.find(index: Int = 0): T {
//    val find  = object : IBlock.IVisitor {
//        var i = 0
//        var e: T? = null
//        override fun visitAny(element: IBlockElement) {
//            if (element is T) {
//                if (e == null && i == index)
//                    e = element
//                i++
//            }
//        }
//    }
//    block.accept(find)
//    return find.e!!
//}

fun <T : IBlockElement> IBlockHolder.find(type: KClass<T>, index: Int = 0): T {
    val find  = object : IBlock.IVisitor {
        var i = 0
        var e: IBlockElement? = null
        override fun visitAny(element: IBlockElement) {
            if (type.isInstance(element)) {
                if (e == null && i == index)
                    e = element
                i++
            }
        }
    }
    block.accept(find)
    return find.e as T
}

fun <T : IBlockElement> IBlockHolder.findAll(type: KClass<T>): List<T> {
    val all = mutableListOf<T>()
    val find  = object : IBlock.IVisitor {
        override fun visitAny(element: IBlockElement) {
            if (type.isInstance(element))
                all.add(type.cast(element))
        }
    }
    block.accept(find)
    return all
}

fun IBlockHolder.contains(e: IBlockElement): Boolean {
    val find  = object : IBlock.IVisitor {
        var found = false
        override fun visitAny(element: IBlockElement) {
            if (element === e)
                found = true
        }
    }
    block.accept(find)
    return find.found
}

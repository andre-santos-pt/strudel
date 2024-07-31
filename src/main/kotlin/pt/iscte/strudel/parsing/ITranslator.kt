package pt.iscte.strudel.parsing

import pt.iscte.strudel.model.IModule
import java.io.File

interface ITranslator {
    fun load(file: File): IModule

    fun load(files: List<File>): IModule

    fun load(src: String): IModule
}
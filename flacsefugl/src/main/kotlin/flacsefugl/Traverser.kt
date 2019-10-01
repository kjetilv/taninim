package flacsefugl

import java.io.File
import java.nio.file.Path

class Traverser(
        private val root: Path,
        private val depth: Int = -1) {

    fun paths(selector: (Path) -> Boolean): List<Path> {
        val dir = root.toFile()
        if (!(dir.exists() && dir.isDirectory)) {
            throw IllegalStateException("Not a good root dir: $dir")
        }
        val fileSelector: (Path) -> Boolean = { path ->
            val file = path.toFile()
            if (file.canRead()) {
                file.isFile && selector(path)
            } else {
                throw IllegalStateException("Unreadable file: $file")
            }
        }
        return listFiles(dir, fileSelector, 0).map {
            it.canonicalFile.toPath()
        }
    }

    private fun listFiles(file: File, selector: (Path) -> Boolean, currentDepth: Int): List<File> {
        if (file.isFile) {
            return listOf(file)
        }
        if (depth > -1 && currentDepth == depth) {
            return emptyList()
        }
        val files =
                file.list { _, name ->
                    selector.invoke(file.toPath().resolve(name))
                }?.map {
                    File(file, it)
                } ?: emptyList()
        val subfiles =
                (file.listFiles()?.filter {
                    it.isDirectory
                } ?: emptyList()).flatMap {
                    listFiles(it, selector, currentDepth + 1)
                }
        return files + subfiles
    }
}

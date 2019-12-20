package flacsefugl

import java.nio.file.Path
import java.nio.file.Paths

class Mover(
        private val rootPath: Path,
        private val targetPath: Path,
        private val dists: List<Dist>
) {

    private val usedIndices = mutableSetOf<Int>()

    fun target(file: Path, newSuffix: String? = null): Path {
        var walker: Path? = file.parent
        var steps = 1
        do {
            if ((walker?.nameCount ?: 0) == 0) {
                return targetFound(file, newSuffix)
            }
            if (rootPath == walker) {
                val subpath = file.subpath(file.nameCount - steps, file.nameCount)
                return targetFound(subpath, newSuffix)
            }
            walker = walker?.parent
            steps += 1
        } while (true)
    }

    fun unused(): List<Dist> =
            dists.withIndex().filter {
                !usedIndices.contains(it.index)
            }.map {
                it.value
            }

    private fun targetFound(file: Path, newSuffix: String?): Path {
        val disted = disted(file)
        return withSuffix(targetPath.resolve(disted), newSuffix)
    }

    private fun disted(path: Path): Path {
        val paths = dists.withIndex().filter {
            it.value.subPath(path) != null
        }
        if (paths.isEmpty()) {
            return path
        }
        val first = paths.first()
        usedIndices.add(first.index)
        return first.value.subPath(path)?.resolve(path) ?: path
    }

    private fun withSuffix(targetFile: Path, newSuffix: String?): Path {
        return newSuffix?.let {
            val name: String = targetFile.getFileName().toString()
            val dot = name.lastIndexOf('.')
            val newFile = Paths.get(name.substring(0, dot) + '.' + newSuffix)
            targetFile.parent.resolve(newFile)
        } ?: targetFile
    }
}

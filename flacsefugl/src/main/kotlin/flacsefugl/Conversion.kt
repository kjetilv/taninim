package flacsefugl

import java.io.File
import java.nio.file.Path

class Conversion(
        val mover: Mover,
        val traverser: Traverser,
        val filter: (Path) -> Boolean
) {
    fun convert(
            newSuffix: String? = null,
            process: (Path, Path) -> Boolean
    ): Boolean {
        println("Validating inputs...")
        val unpaths = traverser.unpaths(filter).map { it.parent.parent.fileName }.distinct().sorted()
        println("${unpaths.size} artists ignored: ${unpaths}}")
        val paths = traverser.paths(filter)
        println("${paths.size} paths to convert")
        val missingDirs: Set<File> = paths.flatMap {
            verifyDirectories(it, newSuffix)
        }.toSet()
        if (missingDirs.isEmpty()) {
            println("All target directories exist")
        } else {
            println("${missingDirs.size} directories to create ...")
            if (created(missingDirs)) {
                println("All ${missingDirs.size} directories created")
            }
        }
        if (mover.unused().isNotEmpty()) {
            mover.unused().forEach {
                println("Not used: $it")
            }
        }
        val conversions = paths.mapIndexed { i, path ->
            println("Processing #$i/${paths.size}: ${path.toFile().absolutePath}")
            process(path, mover.target(path, newSuffix))
        }
        return conversions.all { it }
    }

    private fun created(toCreate: Set<File>) = toCreate.all { it.mkdirs() }

    private fun verifyDirectories(
            path: Path,
            newSuffix: String? = null
    ): Set<File> {
        val sourceFile = path.toFile()
        if (!sourceFile.isFile) {
            throw IllegalArgumentException("Got unexpected non-file: $sourceFile")
        }
        val targetPath = mover.target(path, newSuffix = newSuffix)
        val targetFile = targetPath.toFile()
        if (targetFile.isFile && targetFile.canWrite()) {
            return emptySet()
        }
        val dir = targetPath.parent.toFile()
        if (dir.exists()) {
            if (!dir.canWrite()) {
                throw IllegalStateException("Found non-writable target dir $dir")
            }
            return emptySet()
        }
        return setOf(dir)
    }
}

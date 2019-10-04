package flacsefugl

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    val rootDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/Music/iTunes/iTunes%20Media/Music/"))
//    val targetDir = Paths.get(".")
    val targetDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/FLAC/John%20Zorn"))

    val conversion = Conversion(
            Mover(rootDir, targetDir, dists()),
            Traverser(rootDir)
    ) { path ->
        val name = path.toString().toLowerCase()
        name.endsWith(".m4a") && (
                artist(path).contains("john zorn") ||
                        artist(path).contains("masada") ||
                        album(path).contains("masada") ||
                        album(path).contains("book of angels") ||
                        album(path).contains("book beri") ||
                        album(path).contains("circle maker") ||
                        artist(path).startsWith("naked city") ||
                        artist(path).startsWith("painkiller") ||
                        artist(path).equals("makigami koichi") ||
                        artist(path).equals("derek bailey") ||
                        artist(path).equals("evan parker") ||
                        artist(path).contains("ratkje") ||
                        album(path).contains("great jewish music")
                )
    }

    conversion.convert("flac") { source, target ->
        println("${rootDir.relativize(source)} -> ${targetDir.relativize(target)}")
        true
    }

    conversion.convert("flac") { source, target ->
        if (Files.isRegularFile(target) && Files.size(target) > 0) {
            println("Not converting, already done: $target")
            true
        } else {
            ffmpeg(source, target).await()
        }
    }
}

fun dists(): List<Dist> = listOf<Pair<Path, (Path) -> Boolean>>(

        Paths.get("Masada", "Book of Angels") to { path ->
            album(path).contains("book of angels vol. ")
        },

        Paths.get("Masada", "The Book Beri'ah") to { path ->
            album(path).contains("book beri'ah vol. ")
        },

        Paths.get("Masada", "Masada Book 1") to { path ->
            artist(path) == "masada" && album(path).contains("book 1 vol. ") ||
                    artist(path) == "electric masada"
        },

        Paths.get("Masada", "Masada Book 1", "10. Anniversary") to { path ->
            artist(path) == "masada" && album(path).contains("book 1 vol. ") ||
                    album(path).contains("masada 10. anniversary")
        },

        Paths.get("Masada", "Masada Book 1", "Live") to { path ->
            artist(path) == "masada" && album(path).contains("book 1 vol. ") ||
                    artist(path).contains("masada") && album(path).contains(" live ") ||
                    artist(path).contains("masada") && album(path).contains("50th birthday")
        },

        Paths.get("Masada", "Masada Book 1", "Various") to { path ->
            album(path).contains("circle maker") ||
                    album(path).contains("sanhedrin")
        },

        Paths.get("Hardcore miniatures", "Painkiller") to { path ->
            artist(path).startsWith("painkiller")
        },

        Paths.get("Hardcore miniatures", "Naked City") to { path ->
            artist(path) == "naked city"
        },

        Paths.get("Masada", "Filmworks") to { path ->
            artist(path) == "masada" || artist(path) == "masada string trio"
        },

        Paths.get("Cobra") to { path ->
            album(path).contains("cobra")
        },

        Paths.get("John Zorn") to { path ->
            artist(path).contains("john zorn")
        },

        Paths.get("On Tzadik") to { path ->
            artist(path).equals("makigami koichi") ||
                    artist(path).equals("derek bailey") ||
                    album(path).contains("great jewish music") ||
                    artist(path).equals("fred frith") ||
                    artist(path).equals("evan parker") ||
                    artist(path).contains("ratkje")
        },

        Paths.get("Various") to { path ->
            true
        }
).map {
    SubdirDist(it.first, it.second)
}

private fun album(it: Path) = it.parent.fileName.toString().toLowerCase()

private fun artist(it: Path) = it.parent.parent.fileName.toString().toLowerCase()

private fun ffmpeg(source: Path, target: Path): Command =
        Command(Paths.get("."),
                "/opt/local/bin/ffmpeg", "-i", absOf(source), "-c:a", "flac", absOf(target)
        )

private fun absOf(source: Path) = source.toFile().absolutePath

private fun pathOf(source: Path) = source.toString()

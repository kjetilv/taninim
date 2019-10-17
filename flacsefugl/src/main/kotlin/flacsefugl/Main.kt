package flacsefugl

import mediaserver.files.Media
import java.net.URI
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

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
                        artist(path).contains("bar kokhba") ||
                        artist(path).startsWith("naked city") ||
                        artist(path).startsWith("painkiller") ||
                        artist(path) == "makigami koichi" ||
                        artist(path) == "derek bailey" ||
                        artist(path) == "evan parker" ||
                        artist(path) == "ruins" ||
                        artist(path).contains("bret higgins") ||
                        artist(path).contains("ratkje") ||
                        artist(path).contains("fred frith") ||
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

    val root = Path.of(System.getProperty("user.home"), "FLAC")
    val media = Media.local(root);

    val albumsMetaPath = root.resolve(Path.of("album-meta"))
    val albumsMetaDir = albumsMetaPath.toFile()
    if (albumsMetaDir.isDirectory || albumsMetaDir.mkdir()) {
        media.allAlbums().forEach { album ->
            val target = albumsMetaPath.resolve("album.${album.uuid}")
            val targetFile = target.toFile()
            if (targetFile.isDirectory || targetFile.mkdirs()) {
                println("Created album context dir for ${album.artist.name}/${album.name}: ${targetFile.path}")
            } else {
                throw java.lang.IllegalStateException("Not a directory: $targetFile")
            }
        }
    }

    val objectsPath = root.resolve(Path.of("objects"))
    val objectsDir = objectsPath.toFile()
    if (objectsDir.isDirectory || objectsDir.mkdirs()) {
        media.allTracks.forEach { track ->
            val target = objectsPath.resolve(Path.of("${track.uuid}.flac"))
            val targetFile = target.toFile()
            if (targetFile.isFile && targetFile.length() == track.file.length()) {
                println("Already copied: $track -> $target")
            } else {
                println("Copying $track -> $target")
                copy(track.file.toPath(), target, REPLACE_EXISTING, COPY_ATTRIBUTES)
            }
        }
    } else {
        throw IllegalStateException("Could not create block storage")
    }
}

fun dists(): List<Dist> = listOf<Pair<Path, (Path) -> Boolean>>(

        Paths.get("Masada", "Book of Angels") to albumContains("book of angels vol. "),

        Paths.get("Masada", "The Book Beri'ah") to albumContains("book beri'ah vol. "),

        Paths.get("Masada", "Masada Book 1") to { path ->
            artist(path) == "masada" && album(path).contains("book 1 vol. ")
        },

        Paths.get("Masada", "Various") to { path ->
            album(path).contains("circle maker") ||
                    artist(path).contains("bar kokhba") ||
                    artist(path) == "masada string trio" ||
                    artist(path) == "masada" && album(path).contains("50th birthday")
        },

        Paths.get("Masada", "Masada Book 1", "10. Anniversary") to { path ->
            artist(path) == "masada" && album(path).contains("book 1 vol. ") ||
                    album(path).contains("masada 10. anniversary")
        },

        Paths.get("Masada", "Masada Book 1", "Live") to { path ->
            artist(path).contains("masada") && album(path).contains(" live ")
        },

        Paths.get("Masada", "Various") to artistIs("electric masada"),

        Paths.get("Masada", "Masada Book 1") to albumContains("Sanhedrin"),

        Paths.get("Hardcore miniatures", "Painkiller") to artistContains("painkiller"),

        Paths.get("Hardcore miniatures", "Naked City") to artistIs("Naked City"),

        Paths.get("Cobra") to albumContains("Cobra"),

        Paths.get("John Zorn") to artistContains("John Zorn"),

        Paths.get("On Tzadik") to { path ->
            artist(path) == "makigami koichi" ||
                    artist(path) == "derek bailey" ||
                    album(path).contains("great jewish music") ||
                    artist(path) == "fred frith" ||
                    artist(path) == "evan parker" ||
                    artist(path).contains("bret higgins") ||
                    artist(path).contains("ratkje") ||
                    artist(path) == "ruins"
        },

        Paths.get("Various") to { _ ->
            true
        }
).map {
    SubdirDist(it.first, it.second)
}

private fun artistContains(name: String): (Path) -> Boolean {
    return { path ->
        artist(path).contains(name.toLowerCase())
    }
}

private fun artistIs(name: String): (Path) -> Boolean {
    return { path ->
        artist(path) == name.toLowerCase()
    }
}

private fun albumContains(name: String): (Path) -> Boolean {
    return { path ->
        album(path).contains(name.toLowerCase())
    }
}

private fun album(path: Path) = path.parent.fileName.toString().toLowerCase()

private fun artist(path: Path) = path.parent.parent.fileName.toString().toLowerCase()

private fun ffmpeg(source: Path, target: Path): Command =
        Command(Paths.get("."),
                "/opt/local/bin/ffmpeg", "-i", absOf(source), "-c:a", "flac", absOf(target)
        )


private fun absOf(source: Path) = source.toFile().absolutePath


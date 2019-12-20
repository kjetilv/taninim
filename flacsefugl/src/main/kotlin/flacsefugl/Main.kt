package flacsefugl

import mediaserver.media.CustomCategory
import mediaserver.media.Media
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun main() {
    val rootDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/Music/iTunes/iTunes%20Media/Music/"))
//    val targetDir = Paths.get(".")
    val flacDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/FLAC/John%20Zorn"))
    val m4aDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/M4A/John%20Zorn"))
    val walkmanConnectDir = Paths.get(URI.create(
            "file:///Volumes/WALKMAN"))
    val walkDir = Paths.get(URI.create(
            "file:///Volumes/WALKMAN/MUSIC/John%20Zorn"))
    val included: (Path) -> Boolean = { path: Path ->
        val name = path.toString().toLowerCase()
        name.endsWith(".m4a") && (
                artist(path).contains("john zorn") ||
                        artist(path).contains("masada") ||
                        album(path).contains("masada") ||
                        album(path).contains("book of angels") ||
                        album(path).contains("book beri") ||
                        album(path).contains("circle maker") ||
                        artist(path).contains("bar kokhba") ||
                        artist(path).contains("trigger") ||
                        artist(path).contains("mesinai") ||
                        artist(path).contains("locus solus") ||
                        artist(path).startsWith("naked city") ||
                        artist(path).startsWith("painkiller") ||
                        artist(path) == "makigami koichi" ||
                        artist(path) == "derek bailey" ||
                        artist(path) == "evan parker" ||
                        artist(path) == "ruins" ||
                        artist(path) == "hemophiliac" ||
                        artist(path).contains("ruins") && artist(path).contains("derek") ||
                        artist(path).contains("bret higgins") ||
                        artist(path).contains("bill frisell") ||
                        artist(path).contains("ratkje") ||
                        artist(path).contains("fred frith") ||
                        album(path).contains("great jewish music") ||
                        album(path).contains("spirou")
                )
    }

    val flacConversion = Conversion(
            Mover(rootDir, flacDir, noDists()),
            Traverser(rootDir),
            included)

    flacConversion.convert("flac") { no, total, source, target ->
        println("$no/$total: ${rootDir.relativize(source)} -> ${flacDir.relativize(target)}")
        true
    }

    flacConversion.convert("flac") { no, total, source, target ->
        println("$no/$total: ${source.parent.fileName}/${source.fileName}")
        if (Files.isRegularFile(target) && Files.size(target) > 0 && !changed(source, target)) {
            true
        } else {
            ffmpeg(source, target).await()
        }
    }

    val m4aConversion = Conversion(
            Mover(rootDir, m4aDir, noDists()),
            Traverser(rootDir),
            included)
    m4aConversion.convert("m4a") { no, total, source, target ->
        println("$no/$total: ${source.parent.fileName}/${source.fileName}")
        if (Files.isRegularFile(target) && Files.size(target) > 0 && !changed(source, target)) {
            true
        } else {
            ffmpegM4a(source, target).await()
        }
    }

    if (Files.isDirectory(walkmanConnectDir)) {
        println("Copying to walkman @ $walkDir")
        Files.createDirectories(walkDir)
        Traverser(flacDir).paths { path ->
            path.toString().endsWith(".flac")
        }.forEach { path ->
            val walkFlac = walkDir.resolve(flacDir.relativize(path))
            if (!Files.exists(walkFlac) || Files.size(walkFlac) != Files.size(path)
                    || changed(walkFlac, path)) {
                val dir = walkFlac.toFile().parentFile
                if (dir.isDirectory || dir.mkdirs()) {
                    println("Now walkin: $path")
                    Files.copy(path, walkFlac, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                } else {
                    throw IllegalStateException("Bad target: $dir")
                }
            }
        }
    } else {
        println("Walkman not connected @ $walkDir")
    }

    val media = Media.local(
            Path.of(System.getProperty("user.home"), "FLAC"),
            Path.of(System.getProperty("user.home"), "Music", "iTunes", "iTunes Library.xml"),
            Path.of("mediaserver", "src", "main", "resources"))

    println("Media: $media")

//    media.playlists.
}

private fun changed(source: Path, target: Path): Boolean =
        Files.getLastModifiedTime(source).toInstant().isAfter(
                Files.getLastModifiedTime(target).toInstant())

fun playlists(): List<Dist> =
        CustomCategory.categories("categories.yaml").map { AlbumsDist(it) }

fun noDists(): List<Dist> = emptyList()

fun dists(): List<Dist> = playlists() + listOf<Pair<Path, (Path) -> Boolean>>(

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

        Paths.get("Masada", "Various", "10. Anniversary") to { path ->
            album(path).contains("masada 10. anniversary")
        },

        Paths.get("Masada", "Various", "Live") to { path ->
            artist(path).contains("masada") && album(path).contains(" live ")
        },

        Paths.get("Masada", "Various") to albumContains("Sanhedrin"),

        Paths.get("Masada", "Various") to artistIs("electric masada"),

        Paths.get("Hardcore miniatures", "Painkiller") to artistContains("painkiller"),

        Paths.get("Hardcore miniatures", "Naked City") to artistIs("Naked City"),

        Paths.get("Hardcore miniatures", "Naked City") to albumContains("Spy vs. Spy"),

        Paths.get("John Zorn") to artistContains("John Zorn"),

        Paths.get("John Zorn") to artistContains("Locus Solus"),

        Paths.get("John Zorn") to artistContains("Hemophiliac"),

        Paths.get("On Tzadik") to { path ->
            artist(path) == "makigami koichi" ||
                    artist(path) == "derek bailey" ||
                    album(path).contains("great jewish music") ||
                    artist(path) == "fred frith" ||
                    artist(path) == "evan parker" ||
                    artist(path).contains("bret higgins") ||
                    artist(path).contains("ratkje") ||
                    artist(path) == "ruins" ||
                    artist(path).contains("ruins") && artist(path).contains("derek")
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

private fun ffmpeg(source: Path, target: Path): Command = Command(Paths.get("."),
        "/opt/local/bin/ffmpeg", "-y", "-v", "warning", "-i", absOf(source), "-c:a", "flac", absOf(target))

private fun ffmpegM4a(source: Path, target: Path): Command = Command(Paths.get("."),
        "/opt/local/bin/ffmpeg", "-y", "-v", "warning", "-i", absOf(source), "-c:a", "aac", "-b:a", "128k", absOf(target))


private fun absOf(source: Path) = source.toFile().absolutePath


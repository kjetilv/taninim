package flacsefugl

import mediaserver.gui.Template
import mediaserver.http.QPar
import mediaserver.media.CustomCategory
import mediaserver.media.Media
import mediaserver.media.PlaylistM3U
import mediaserver.util.IO
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
    val flac2Dir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/FLAC2/John%20Zorn"))
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

    val dirStruct = Conversion(
            Mover(rootDir, flac2Dir, playlists()),
            Traverser(rootDir),
            included)

    dirStruct.convert("flac") { no: Int, total: Int, source: Path, target: Path ->
        println(target)
        true;
    }

    val flacConversion = Conversion(
            Mover(rootDir, flacDir, noDists()),
            Traverser(rootDir),
            included)

    flacConversion.convert("flac") { no, total, source, target ->
        if (no % 100 == 0) {
            println("$no/$total: ${rootDir.relativize(source)} -> ${flacDir.relativize(target)}")
        }
        true
    }

    flacConversion.convert("flac") { no, total, source, target ->
        if (no % 100 == 0) {
            println("$no/$total: ${source.parent.fileName}/${source.fileName}")
        }
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
        if (no % 100 == 0) {
            println("$no/$total: ${source.parent.fileName}/${source.fileName}")
        }
        if (Files.isRegularFile(target) && Files.size(target) > 0 && !changed(source, target)) {
            true
        } else {
            ffmpegM4a(source, target).await()
        }
    }

    if (Files.isDirectory(walkmanConnectDir)) {
        println("Copying to walkman @ $walkDir")
        Files.createDirectories(walkDir)
        val mover = Mover(flacDir, walkDir, playlists())
        Traverser(flacDir).paths { path ->
            path.toString().endsWith(".flac")
        }.forEach { path ->
            val walkFlac = walkDir.resolve(flacDir.relativize(path))
//                    mover.target(path)
//            println("Move: $path => ${mover.target(path)}")
            if (!Files.exists(walkFlac) || Files.size(walkFlac) != Files.size(path) || changed(walkFlac, path)) {
                val dir = walkFlac.toFile().parentFile
                if (dir.isDirectory || dir.mkdirs()) {
                    println("Now walkin: $walkFlac")
                    Files.copy(path, walkFlac, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
                } else {
                    throw IllegalStateException("Bad target: $dir")
                }
            }
        }
    } else {
        println("Walkman not connected for files @ $walkmanConnectDir")
    }

    val media = Media.local(
            Path.of(System.getProperty("user.home"), "FLAC"),
            Path.of(System.getProperty("user.home"), "Music", "iTunes", "iTunes Library.xml"),
            Path.of("mediaserver", "src", "main", "resources"))

    println("Media: $media")

    if (Files.isDirectory(walkmanConnectDir)) {
        val source = IO.read("playlist.m3u8").unpack().orElseThrow { ->
            IllegalStateException("No source @ playlist.m3u8")
        }
        val sourceDir = Path.of(System.getProperty("user.home"), "FLAC")
        val musicDir = walkmanConnectDir.resolve("MUSIC")
        media.playlists.forEach { playlist ->
            val playlistM3U = PlaylistM3U(playlist.name, playlist.tracks).move(sourceDir)
            val template = Template(playlist.name, source)
            val bytes = template.add(QPar.PLAYLIST, playlistM3U).bytes()
            val replacedName =
                    playlist.name.replace('/', ' ') + ".M3U8"
            val target = musicDir.resolve(replacedName)
            Files.write(target, bytes);
            println("Playlist: ${playlist.name}: $target")
        }
    } else {
        println("Walkman not connected for playlists @ $walkmanConnectDir")
    }
}

private fun changed(source: Path, target: Path): Boolean =
        Files.getLastModifiedTime(source).toInstant().isAfter(
                Files.getLastModifiedTime(target).toInstant())

fun playlists(): List<Dist> =
        CustomCategory.categories("categories.yaml").map { AlbumsDist(it) }

fun noDists(): List<Dist> = emptyList()

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


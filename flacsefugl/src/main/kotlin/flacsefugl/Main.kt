package flacsefugl

import mediaserver.media.Media
import mediaserver.media.PlaylistM3U
import mediaserver.media.PlaylistYaml
import mediaserver.templates.TPar
import mediaserver.templates.Template
import mediaserver.util.IO
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption
import java.util.*

fun main() {
    val rootDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/Music/iTunes/iTunes%20Media/Music/"))
    val flacDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/FLAC/John%20Zorn"))
    val m4aDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/M4A/John%20Zorn"))
    val walkmanConnectDir = Paths.get(URI.create(
            "file:///Volumes/WALKMAN"))
    val musicDir =
            walkmanConnectDir.resolve("MUSIC")
    val walkDir =
            musicDir.resolve("John Zorn")
    val included: (Path) -> Boolean = { path: Path ->
        val name = path.toString().toLowerCase()
        name.endsWith(".m4a") && (
                artist(path).contains("john zorn") ||
                        artist(path).contains("masada") ||
                        album(path).contains("masada") ||
                        album(path).contains("the stone issue") ||
                        album(path).contains("book of angels") ||
                        album(path).contains("book beri") ||
                        album(path).contains("circle maker") ||
                        artist(path).contains("bar kokhba") ||
                        artist(path).contains("medeski") ||
                        artist(path).contains("cracow") ||
                        artist(path).contains("trigger") ||
                        artist(path).contains("mesinai") ||
                        artist(path).contains("autoryno") ||
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
                        album(path).contains("buck jam tonic") ||
                        album(path).contains("spirou")
                )
    }

    val flacConversion = Conversion(
            Mover(rootDir, flacDir, noDists()),
            Traverser(rootDir),
            included)

    val flacConversions = flacConversion.convert("flac") { _, _, source, target ->
        if (shouldUpdate(source, target)) {
            ffmpeg(source, target).await()
        }
    }
    removeLeftovers(flacConversions, flacDir)

    val m4aCompression = Conversion(
            Mover(rootDir, m4aDir, noDists()),
            Traverser(rootDir),
            included)

    val compressions = m4aCompression.convert("m4a") { _, _, source, target ->
        if (shouldUpdate(source, target)) {
            ffmpegM4a(source, target).await()
        }
    }
    removeLeftovers(compressions, m4aDir)

    val media = Media.local(
            Path.of(System.getProperty("user.home"), "FLAC"),
            Path.of(System.getProperty("user.home"), "Downloads", "Library.xml"),
            Path.of("mediaserver", "src", "main", "resources"))

    println("Media: $media")

    media.albumContexts.forEach { albumContext ->
        val discogId = albumContext.discogId
        val dir = albumContext.album.path
        val coverPath = if (discogId == null)
            Optional.empty<Path>()
        else IO.readBytes("releases/$discogId.jpg")
                .unpackTyped(IO.Type.SOURCES) { bytes ->
                    Files.write(dir.resolve("cover.jpg"), bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
                }
        println("Cover: $coverPath")
    }

    if (Files.isDirectory(walkmanConnectDir)) {
        val path = Files.createDirectories(walkDir)
        println("Copying files to to walkman @ $path")
        val walkmanTransfer = Conversion(
                Mover(flacDir, walkDir, playlists()),
                Traverser(flacDir))
        val transfers =
                walkmanTransfer.convert { _: Int, _: Int, source: Path, target: Path ->
                    if (shouldUpdate(source, target)) {
                        println("$source -> $target")
                        Files.copy(source, target, COPY_ATTRIBUTES, REPLACE_EXISTING)
                    }
                }
        removeLeftovers(transfers, walkDir)

        println("Copying playlists...")
        val source = IO.readUTF8("playlist.m3u8").unpack().orElseThrow { ->
            IllegalStateException("No source @ playlist.m3u8")
        }

        media.playlists.forEach { playlist ->
            val playlistM3U = PlaylistM3U(playlist.name, playlist.tracks)
                    .move(musicDir) { track ->
                        val mover = Mover(flacDir, walkDir, playlists())
                        Optional.ofNullable(mover.target(track))
                    }
            val template = Template(playlist.name, source)
            val bytes = template.add(TPar.playlist, playlistM3U).bytes()
            val replacedName = playlist.name.replace('/', ' ') + ".M3U8"
            val target = musicDir.resolve(replacedName)
            Files.write(target, bytes)
            println("Playlist: ${playlist.name}: $target")
        }

        deleteEmpty(walkDir.toFile())
    } else {
        println("Walkman not connected for playlists @ $walkmanConnectDir")
    }
}

private fun deleteEmpty(f: File) {
    if (f.isDirectory && f.listFiles()?.isEmpty() ?: true) {
        println("Deleting empty dir: ${f.absolutePath}")
        f.delete()
    } else {
        f.listFiles()?.forEach { sub ->
            deleteEmpty(sub)
        }
    }
}

private fun removeLeftovers(conversions: List<Pair<Path, Path>>, targetDir: Path) {
    val convertedTargets = conversions.map { it.second }
    Traverser(targetDir).paths().filter { target ->
        !convertedTargets.contains(target)
    }.forEach {
        Files.deleteIfExists(it)
    }
}

private fun shouldUpdate(source: Path, target: Path) =
        !Files.isRegularFile(target) || Files.size(target) <= 0 || changed(source, target)

private fun playlists(): List<Dist> =
        PlaylistYaml.playlists("categories.yaml").map { AlbumsDist(it) }

private fun noDists(): List<Dist> = emptyList()

private fun changed(source: Path, target: Path): Boolean =
        Files.getLastModifiedTime(source).toInstant().isAfter(
                Files.getLastModifiedTime(target).toInstant().plusSeconds(5))

private fun album(path: Path) = path.parent.fileName.toString().toLowerCase()

private fun artist(path: Path) = path.parent.parent.fileName.toString().toLowerCase()

private fun ffmpeg(source: Path, target: Path): Command = Command(Paths.get("."),
        "/opt/local/bin/ffmpeg", "-y", "-v", "warning", "-i", absOf(source), "-c:a", "flac", absOf(target))

private fun ffmpegM4a(source: Path, target: Path): Command = Command(Paths.get("."),
        "/opt/local/bin/ffmpeg", "-y", "-v", "warning", "-i", absOf(source), "-c:a", "aac", "-b:a", "128k",
        absOf(target))

private fun absOf(source: Path) = source.toFile().absolutePath


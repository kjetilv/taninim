package flacsefugl

import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val targetDir = Paths.get(URI.create(
            "file://${System.getProperty("user.home")}/tmp"
    ))
    Files.delete(targetDir)
}

package flacsefugl

import java.nio.file.Path
import java.util.concurrent.TimeUnit

class Command(pwd: Path, vararg cmd: String) {

    private val process = ProcessBuilder(cmd.toList())
            .directory(pwd.toFile())
            .inheritIO()
//            .redirectError(ProcessBuilder.Redirect.DISCARD)
//            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

    fun await(): Boolean =
            try {
                process.waitFor(5, TimeUnit.MINUTES)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Interrupted", e)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to convert")
            }
}

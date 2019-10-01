package flacsefugl

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class MoverTest {

    @Test
    fun test() {
        val mover = Mover(
                Paths.get("foo", "bar", "zot"),
                Paths.get("zip", "zot"))

        assertEquals(
                Paths.get("zip", "zot", "add", "bar.wav"),
                mover.target(
                        Paths.get("foo", "bar", "zot", "add", "bar.wav")))

        assertEquals(
                Paths.get("zip", "zot", "add", "bar.flac"),
                mover.target(
                        Paths.get("foo", "bar", "zot", "add", "bar.wav"),
                        "flac"))

        assertEquals(
                Paths.get("zip", "zot", "add", "bar.wav"),
                mover.target(
                        Paths.get("add", "bar.wav")))

        assertEquals(
                Paths.get("zip", "zot", "add", "bar.flash"),
                mover.target(
                        Paths.get("add", "bar.wav"),
                        "flash"))

        assertEquals(
                mover.target(
                        Paths.get("bar.wav")),
                Paths.get("zip", "zot", "bar.wav"))
    }

}

package flacsefugl

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class TraverserTest {

    @Test
    fun can_traverse() {
        val paths = Traverser(Paths.get(".")).paths {
            true
        }
        assertTrue(paths.size > 0)
    }
}

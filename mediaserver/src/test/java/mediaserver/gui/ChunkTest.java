package mediaserver.gui;

import mediaserver.toolkit.Chunk;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChunkTest {

    @Test
    public void test_chunk() {

        Chunk chunk = new Chunk(0, 1, 2000);
        assertEquals("0-0/2000", chunk.getRange());
    }

}

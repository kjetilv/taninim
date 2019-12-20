package mediaserver.files;

import mediaserver.media.CategoryPath;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class CategoryPathTest {

    @Test
    public void testBelonging() {

        assertTrue(
            new CategoryPath(Path.of("foo", "bar")).startsWith(
                new CategoryPath(Path.of("foo"))
            ));
    }
}

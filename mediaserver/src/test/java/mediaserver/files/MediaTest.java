package mediaserver.files;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class MediaTest {

    @Test
    public void listall() {
        Media media = new Media(Path.of(System.getProperty("user.home"), "FLAC"));
        System.out.print(media);
    }
}

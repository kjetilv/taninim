package mediaserver.files;

import mediaserver.Media;
import org.junit.Test;

import java.nio.file.Path;

public class MediaTest {

    @Test
    public void listall() {

        Media media = Media.local(
            Path.of(System.getProperty("user.home"), "FLAC"),
            Path.of(System.getProperty("user.home"), "Music", "iTunes", "iTunes Library.xml"),
            Path.of("mediaserver", "src", "main", "resources")
        );
        media.getAlbums().stream().map(Album::toStringBody).forEach(System.out::println);
    }
}

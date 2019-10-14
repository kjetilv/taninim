package mediaserver.files;

import org.junit.Test;

import java.nio.file.Path;

public class MediaTest {

    @Test
    public void listall() {
        Media media = Media.at(Path.of(System.getProperty("user.home"), "FLAC"));
        media.getAlbums().stream().map(Album::print).forEach(System.out::println);
    }
}

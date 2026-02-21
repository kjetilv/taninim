package taninim.music.medias;

import module java.base;
import taninim.music.aural.Chunk;

public interface MediaLibrary {

    Optional<Long> fileSize(String file);

    Optional<? extends InputStream> stream(String file);

    Optional<? extends InputStream> stream(Chunk chunk, String file);

    void write(String file, Consumer<? super OutputStream> writer);
}

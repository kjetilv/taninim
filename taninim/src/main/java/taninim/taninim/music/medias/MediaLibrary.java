package taninim.taninim.music.medias;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import taninim.taninim.music.aural.Chunk;

public interface MediaLibrary {

    Optional<Long> fileSize(String file);

    Optional<? extends InputStream> stream(String file);

    Optional<? extends InputStream> stream(Chunk chunk, String file);

    void write(String file, Consumer<? super OutputStream> writer);
}

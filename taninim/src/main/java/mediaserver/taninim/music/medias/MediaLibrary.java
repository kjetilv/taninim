package mediaserver.taninim.music.medias;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import mediaserver.taninim.music.aural.Chunk;

public interface MediaLibrary {

    Optional<Long> fileSize(String file);

    Optional<? extends InputStream> stream(String file);

    Optional<InputStream> write(String file, Consumer<? super OutputStream> writer);

    Optional<? extends InputStream> stream(Chunk chunk, String file);
}

package taninim.kudu;

import module java.base;
import module taninim.taninim;
import module uplift.kernel;
import module uplift.uuid;

public interface Kudu {

    default Optional<byte[]> library(Uuid token) {
        return libraryStream(token).map(Library::stream).map(BytesIO::readInputStream);
    }

    Optional<Library> libraryStream(Uuid token);

    Optional<AudioBytes> audioBytes(TrackRange trackRange);

    Optional<AudioStream> audioStream(TrackRange trackRange);

    record Library(
        long size,
        InputStream stream
    ) {

    }

    record AudioStream(
        TrackRange trackRange,
        Chunk chunk,
        InputStream stream
    ) {

    }

    record AudioBytes(
        TrackRange trackRange,
        Chunk chunk,
        byte[] bytes
    ) {

    }
}

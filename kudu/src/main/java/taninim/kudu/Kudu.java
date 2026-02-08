package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import taninim.music.aural.Chunk;

import static com.github.kjetilv.uplift.hash.HashKind.K128;

public interface Kudu {

    default Optional<byte[]> library(Hash<K128> token) {
        return libraryStream(token)
            .map(Library::stream)
            .map(BytesIO::readInputStream);
    }

    Optional<Library> libraryStream(Hash<K128> token);

    Optional<AudioBytes> audioBytes(TrackRange trackRange);

    Optional<AudioStream> audioStream(TrackRange trackRange);

    record Library(long size, InputStream stream) {
    }

    record AudioStream(TrackRange trackRange, Chunk chunk, InputStream stream) {
    }

    record AudioBytes(TrackRange trackRange, Chunk chunk, byte[] bytes) {
    }
}

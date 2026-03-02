package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import taninim.auth.Authed;
import taninim.music.LeasesRegistry;
import taninim.music.aural.Chunk;
import taninim.music.medias.MediaLibrary;

public final class DefaultKudu implements Kudu {

    private final LeasesRegistry leasesRegistry;

    private final MediaLibrary mediaLibrary;

    private final int transferSize;

    private final Supplier<Instant> time;

    DefaultKudu(
        LeasesRegistry leasesRegistry,
        MediaLibrary mediaLibrary,
        int transferSize,
        Supplier<Instant> time
    ) {
        this.leasesRegistry = leasesRegistry;
        this.mediaLibrary = mediaLibrary;
        this.transferSize = transferSize;
        this.time = time;
    }

    @Override
    public Authed<Library> libraryStream(Hash<HashKind.K128> token) {
        return leasesRegistry.active(token)
            .flatMap(_ ->
                Authed.resolve(mediaLibrary.fileSize("media.jsonl.gz").flatMap(size ->
                    mediaLibrary.stream(null, "media.jsonl.gz")
                        .map(inputStream ->
                            new Library(size, inputStream)))));
    }

    @Override
    public Authed<AudioBytes> audioBytes(TrackRange trackRange) {
        return leasesRegistry.active(trackRange.token())
            .filterOr(
                leasesPath ->
                    leasesPath.leases().validFor(trackRange.track().trackUUID(), time.get()),
                () ->
                    Authed.unauthorized("No lease for " + trackRange))
            .flatMap(_ ->
                chunk(trackRange, transferSize)
                    .flatMap(byteReader(trackRange)));
    }

    private Function<Chunk, Authed<AudioBytes>> byteReader(TrackRange trackRange) {
        return chunk ->
            Authed.resolve(mediaLibrary.stream(chunk, trackRange.track().file()))
                .map(BytesIO::readInputStream)
                .map(bytes ->
                    new AudioBytes(trackRange, chunk, bytes));
    }

    private Authed<Chunk> chunk(TrackRange trackRange, int transferSize) {
        return Authed.resolve(mediaLibrary.fileSize(trackRange.track().file()))
            .flatMap(fileSize ->
                Chunk.create(
                    trackRange.range().withLength(fileSize),
                    trackRange.track().format().suffix(),
                    transferSize
                ));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "[leasesRegistry:" + leasesRegistry +
               " mediaLibrary:" + mediaLibrary +
               " transferSize:" + transferSize +
               "]";
    }
}

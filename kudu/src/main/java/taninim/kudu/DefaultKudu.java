package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.music.LeasesRegistry;
import taninim.music.aural.Chunk;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.MediaLibrary;

public final class DefaultKudu implements Kudu {

    public static Kudu create(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory
    ) {
        var s3Accessor = s3AccessorFactory.create();
        var time = clientSettings.time();
        var archives = S3Archives.create(s3Accessor);
        var leasesRegistry =
            ArchivedLeasesRegistry.create(
                archives,
                taninimSettings.leaseDuration(),
                time
            );
        var mediaLibrary = CloudMediaLibrary.create(s3Accessor, time);
        return create(leasesRegistry, mediaLibrary, taninimSettings.transferSize(), time);
    }

    public static DefaultKudu create(
        LeasesRegistry leasesRegistry,
        MediaLibrary mediaLibrary,
        int transferSize,
        Supplier<Instant> time
    ) {
        return new DefaultKudu(leasesRegistry, mediaLibrary, transferSize, time);
    }

    private final LeasesRegistry leasesRegistry;

    private final MediaLibrary mediaLibrary;

    private final int transferSize;

    private final Supplier<Instant> time;

    private DefaultKudu(
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
    public Optional<Library> libraryStream(Hash<HashKind.K128> token) {
        return leasesRegistry.getActive(token).flatMap(_ ->
            mediaLibrary.fileSize("media.jsonl.gz").flatMap(size ->
                mediaLibrary.stream(null, "media.jsonl.gz")
                    .map(inputStream ->
                        new Library(size, inputStream))));
    }

    @Override
    public Optional<AudioBytes> audioBytes(TrackRange trackRange) {
        return bytes(trackRange, byteReader(trackRange));
    }

    @Override
    public Optional<AudioStream> audioStream(TrackRange trackRange) {
        return bytes(trackRange, byteStreamer(trackRange));
    }

    private <T> Optional<T> bytes(TrackRange trackRange, Function<? super Chunk, Optional<T>> streamer) {
        return leasesRegistry.getActive(trackRange.token())
            .filter(leasesPath ->
                leasesPath.leases().validFor(trackRange.track().trackUUID(), time.get()))
            .flatMap(_ ->
                chunk(trackRange, transferSize)
                    .flatMap(streamer));
    }

    private Function<Chunk, Optional<AudioBytes>> byteReader(TrackRange trackRange) {
        return chunk ->
            mediaLibrary.stream(chunk, trackRange.track().file())
                .map(BytesIO::readInputStream)
                .map(bytes ->
                    new AudioBytes(trackRange, chunk, bytes));
    }

    private Function<Chunk, Optional<AudioStream>> byteStreamer(TrackRange trackRange) {
        return chunk ->
            mediaLibrary.stream(chunk, trackRange.track().file())
                .map(bytes ->
                    new AudioStream(trackRange, chunk, bytes));
    }

    private Optional<Chunk> chunk(TrackRange trackRange, int transferSize) {
        return mediaLibrary.fileSize(trackRange.track().file()).flatMap(fileSize ->
            Chunk.create(trackRange.range().withLength(fileSize), trackRange.track().format().suffix(), transferSize));
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

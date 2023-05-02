package taninim.kudu;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.taninim.TaninimSettings;
import taninim.taninim.music.Archives;
import taninim.taninim.music.LeasesRegistry;
import taninim.taninim.music.aural.Chunk;
import taninim.taninim.music.legal.ArchivedLeasesRegistry;
import taninim.taninim.music.legal.CloudMediaLibrary;
import taninim.taninim.music.legal.S3Archives;
import taninim.taninim.music.medias.MediaLibrary;

public record DefaultKudu(
    LeasesRegistry leasesRegistry,
    MediaLibrary mediaLibrary,
    int transferSize,
    Supplier<Instant> time
) implements Kudu {

    static Kudu create(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory
    ) {
        S3Accessor s3Accessor = s3AccessorFactory.create();
        Supplier<Instant> time = clientSettings.time();
        Archives archives = new S3Archives(s3Accessor);
        LeasesRegistry leasesRegistry =
            new ArchivedLeasesRegistry(
                archives,
                taninimSettings.leaseDuration(),
                time,
                clientSettings.serverExecutor()
            );
        MediaLibrary mediaLibrary = new CloudMediaLibrary(s3Accessor, time);
        return new DefaultKudu(leasesRegistry, mediaLibrary, taninimSettings.transferSize(), time);
    }

    @Override
    public Optional<Library> libraryStream(Uuid token) {
        return leasesRegistry.getActive(token).flatMap(ticket ->
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
                leasesPath.leases().validFor(trackRange.trackUUID(), time.get()))
            .flatMap(validLease ->
                chunk(trackRange, transferSize)
                    .flatMap(streamer));
    }

    private Function<Chunk, Optional<AudioBytes>> byteReader(TrackRange trackRange) {
        return chunk ->
            mediaLibrary.stream(chunk, trackRange.file())
                .map(BytesIO::readInputStream)
                .map(bytes ->
                    new AudioBytes(trackRange, chunk, bytes));
    }

    private Function<Chunk, Optional<AudioStream>> byteStreamer(TrackRange trackRange) {
        return chunk ->
            mediaLibrary.stream(chunk, trackRange.file())
                .map(bytes ->
                    new AudioStream(trackRange, chunk, bytes));
    }

    private Optional<Chunk> chunk(TrackRange trackRange, int transferSize) {
        return mediaLibrary.fileSize(trackRange.file()).flatMap(fileSize ->
            Chunk.create(trackRange.range().withLength(fileSize), trackRange.format(), transferSize));
    }
}

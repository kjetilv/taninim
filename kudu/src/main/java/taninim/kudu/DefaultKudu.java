package taninim.kudu;

import module java.base;
import module taninim.taninim;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.uuid;

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
        return new DefaultKudu(leasesRegistry, mediaLibrary, taninimSettings.transferSize(), time);
    }

    @Override
    public Optional<Library> libraryStream(Uuid token) {
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
            .flatMap(validLease ->
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
}

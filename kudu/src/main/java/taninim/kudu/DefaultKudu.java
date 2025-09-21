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
        S3Accessor s3Accessor = s3AccessorFactory.create();
        Supplier<Instant> time = clientSettings.time();
        Archives archives = new S3Archives(s3Accessor);
        LeasesRegistry leasesRegistry =
            new ArchivedLeasesRegistry(
                archives,
                taninimSettings.leaseDuration(),
                time
            );
        MediaLibrary mediaLibrary = new CloudMediaLibrary(s3Accessor, time);
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

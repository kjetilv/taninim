package taninim.music.legal;

import module java.base;
import module taninim.taninim;
import module uplift.s3;

import static java.util.Objects.requireNonNull;

public final class S3Archives implements Archives {

    private final S3Accessor s3;

    public static Archives create(S3Accessor s3) {
        return new S3Archives(s3);
    }

    private S3Archives(S3Accessor s3) {
        this.s3 = requireNonNull(s3, "s3");
    }

    @Override
    public void storeRecord(ArchivedRecord archivedRecord) {
        s3.put(archivedRecord.body(), archivedRecord.path());
    }

    @Override
    public Optional<ArchivedRecord> retrieveRecord(String path) {
        return s3.stream(path)
            .map(inputStream ->
                new ArchivedRecord(path, stream(path, inputStream)));
    }

    @Override
    public Stream<String> retrievePaths(String prefix, Predicate<? super String> filter) {
        var paths = s3.list(requireNonNull(prefix, "prefix"));
        return filter == null
            ? paths
            : paths.filter(filter);
    }

    @Override
    public void clearRecords(Collection<String> paths) {
        s3.remove(paths);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + s3 + "]";
    }

    private static List<String> stream(String path, InputStream is) {
        try (
            var reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            var bufferedReader = new BufferedReader(reader)
        ) {
            return bufferedReader.lines()
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}

package taninim.music.legal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.music.Archives;

import static java.util.Objects.requireNonNull;

public class S3Archives implements Archives {

    private final S3Accessor s3;

    public S3Archives(S3Accessor s3) {
        this.s3 = requireNonNull(s3, "s3");
    }

    @Override
    public void storeRecord(ArchivedRecord archivedRecord) {
        s3.put(archivedRecord.body(), archivedRecord.path());
    }

    @Override
    public Optional<ArchivedRecord> retrieveRecord(String path) {
        return read(path);
    }

    @Override
    public Stream<String> retrievePaths(String prefix, Predicate<? super String> filter) {
        Stream<String> paths = s3.list(requireNonNull(prefix, "prefix"));
        return filter == null
            ? paths
            : paths.filter(filter);
    }

    @Override
    public void clearRecords(Collection<String> paths) {
        s3.remove(paths);
    }

    private Optional<ArchivedRecord> read(String path) {
        return s3.stream(path).map(is -> {
            try (
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(reader)
            ) {
                return new ArchivedRecord(path, bufferedReader.lines().toList());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read " + path, e);
            }
        });
    }
}
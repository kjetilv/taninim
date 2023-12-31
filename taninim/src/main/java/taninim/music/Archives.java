package taninim.music;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.uuid.Uuid;

public interface Archives {

    void storeRecord(ArchivedRecord archivedRecord);

    Optional<ArchivedRecord> retrieveRecord(String path);

    Stream<String> retrievePaths(String prefix, Predicate<? super String> filter);

    void clearRecords(Collection<String> paths);

    record ArchivedRecord(
        String path,
        List<String> contents,
        int length
    ) {

        public ArchivedRecord(String path, List<String> contents) {
            this(path, contents, -1);
        }

        public ArchivedRecord(String path, List<String> contents, int length) {
            this.path = path;
            this.contents = contents.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
            this.length = length < 0 ? this.contents.stream().mapToInt(String::length).sum() : length;
        }

        public String body() {
            StringBuilder sb = new StringBuilder(length + contents.size());
            contents.forEach(line ->
                sb.append(line.trim()).append('\n'));
            return sb.toString();
        }

        private static String printEntry(String spec) {
            Uuid uuid = Uuid.from(spec);
            long epochSec = Long.parseLong(spec.substring(spec.indexOf(' ') + 1));
            return uuid + "@" + Instant.ofEpochSecond(epochSec).atOffset(ZoneOffset.UTC).format(
                DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + path + (empty()
                ? ""
                : ": " + contents.stream()
                    .map(ArchivedRecord::printEntry)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "))) + "]";
        }

        private boolean empty() {
            return contents == null || contents.isEmpty() || contents.stream().allMatch(String::isBlank);
        }
    }
}

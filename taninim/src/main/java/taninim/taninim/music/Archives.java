package taninim.taninim.music;

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

import com.github.kjetilv.uplift.kernel.uuid.Uuid;

public interface Archives {

    void storeRecord(ArchivedRecord archivedRecord);

    Optional<ArchivedRecord> retrieveRecord(String path);

    Stream<String> retrievePaths(String prefix, Predicate<? super String> filter);

    void clearRecords(Collection<String> paths);

    record ArchivedRecord(
        String path,
        List<String> contents
    ) {

        public String body() {
            StringBuilder stringBuilder = new StringBuilder();
            contents.forEach(line -> {
                stringBuilder.append(line.trim());
                stringBuilder.append('\n');
            });
            return stringBuilder.append('\n').toString();
        }

        private static String printEntry(String spec) {
            Uuid uuid = Uuid.from(spec);
            long epochSec = Long.parseLong(spec.substring(spec.indexOf(' ') + 1));
            return uuid + "@" + Instant.ofEpochSecond(epochSec).atOffset(ZoneOffset.UTC).format(
                DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + path + (contents == null || contents.isEmpty()
                ? ""
                : ": " + contents.stream()
                    .map(ArchivedRecord::printEntry)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "))) + "]";
        }
    }
}

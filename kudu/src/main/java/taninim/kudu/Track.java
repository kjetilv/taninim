package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static java.util.Objects.requireNonNull;
import static taninim.util.ParseBits.tailString;

public record Track(Hash<K128> trackUUID, Format format) {

    public static Optional<Track> parse(String audioFile) {
        return format(audioFile).map(format ->
            new Track(Hash.from(audioFile), format));
    }

    public Track {
        requireNonNull(trackUUID, "trackUUID");
        requireNonNull(format, "format");
    }

    String file() {
        return trackUUID.asUuid() + "." + format.suffix();
    }

    private static Optional<Format> format(String audioId) {
        return tailString(audioId, K128.totalDigestLength())
            .map(Track::lc)
            .flatMap(Track::toFormat);
    }

    private static Optional<Format> toFormat(String tail) {
        return Arrays.stream(Format.values())
            .filter(format -> format.matches(tail))
            .findFirst();
    }

    private static String lc(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public enum Format {
        FLAC,
        M4A;

        private final String suffix;

        Format() {
            this.suffix = lc(name());
        }

        public String suffix() {
            return suffix;
        }

        public boolean matches(String fileName) {
            return fileName.endsWith(suffix());
        }
    }
}

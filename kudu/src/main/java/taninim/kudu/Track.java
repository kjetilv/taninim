package taninim.kudu;

import module java.base;
import module uplift.uuid;

import static java.util.Objects.requireNonNull;
import static taninim.util.ParseBits.tailString;

public record Track(Uuid trackUUID, Format format) {

    public static Optional<Track> parse(String audioFile) {
        return format(audioFile).map(format ->
            new Track(Uuid.from(audioFile), format));
    }

    public Track {
        requireNonNull(trackUUID, "trackUUID");
        requireNonNull(format, "format");
    }

    String file() {
        return trackUUID.uuid() + "." + format.suffix();
    }

    private static Optional<Format> format(String audioId) {
        return tailString(audioId, Uuid.DIGEST_LENGTH + 1)
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

package mediaserver.media;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PlaylistEntry {

    private final Collection<Matcher> artistSpecs;

    private final Collection<Matcher> albumSpecs;

    private final Collection<Matcher> trackSpecs;

    public PlaylistEntry(String spec) {

        Collection<Matcher> specs = Arrays.stream(Objects.requireNonNull(spec, "spec")
            .split("\\s+&&\\s+")
        ).map(Matcher::new).collect(Collectors.toList());
        this.artistSpecs = specs(specs, MatchType.ARTIST);
        this.albumSpecs = specs(specs, MatchType.ALBUM);
        this.trackSpecs = specs(specs, MatchType.TRACK);
    }

    public boolean match(Album album) {

        return artistMatch(album.getArtist().getName()) && albumMatch(album.getName());
    }

    public boolean match(Path path) {

        return artistMatch(artist(path)) &&
            albumMatch(album(path)) &&
            trackMatch(track(path));
    }

    private static String track(Path path) {

        return path.getFileName().toString();
    }

    private static String album(Path path) {

        return path.getParent().getFileName().toString();
    }

    private static String artist(Path path) {

        return path.getParent().getParent().getFileName().toString();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[art:" + artistSpecs +
            " alb:" + albumSpecs +
            " trl:" + trackSpecs +
            "]";
    }

    private static List<Matcher> specs(Collection<Matcher> spec, MatchType matchType) {

        return spec.stream()
            .filter(m ->
                m.getType() == matchType)
            .collect(Collectors.toList());
    }

    private boolean trackMatch(String track) {

        return match(track, trackSpecs);
    }

    private boolean albumMatch(String album) {

        return match(album, this.albumSpecs);
    }

    private boolean artistMatch(String artist) {

        return match(artist, artistSpecs);
    }

    private static boolean match(String str, Collection<Matcher> specs) {

        return specs.isEmpty() || specs.stream().allMatch(m -> m.test(str));
    }

    enum MatchType {

        ARTIST, ALBUM, TRACK
    }

    public static class Matcher implements Predicate<String> {

        private final MatchType type;

        private final String matchValue;

        public Matcher(String matchValue) {

            this.type = matchValue.startsWith("/") ? MatchType.TRACK
                : matchValue.endsWith("/") ? MatchType.ARTIST
                : MatchType.ALBUM;
            this.matchValue = (type == MatchType.ARTIST ? matchValue.substring(0, matchValue.length() - 1)
                : type == MatchType.TRACK ? matchValue.substring(1)
                : matchValue).toLowerCase();
        }

        @Override
        public boolean test(String s) {

            return s.toLowerCase().contains(matchValue);
        }

        public MatchType getType() {

            return type;
        }

        @Override
        public String toString() {

            return getClass().getSimpleName() + "[" + type + " " + matchValue + "]";
        }
    }
}

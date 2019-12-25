package mediaserver.media;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PlaylistEntry {

    private final Collection<Matcher> spec;

    private final Collection<Matcher> artistSpecs;

    private final Collection<Matcher> albumSpecs;

    private final Collection<Matcher> trackSpecs;

    public PlaylistEntry(String spec) {

        this.spec = Arrays.stream(Objects.requireNonNull(spec, "spec").split("\\s+&&\\s+"))
            .map(Matcher::new)
            .collect(Collectors.toList());
        this.artistSpecs = this.spec.stream().filter(m -> m.getType() == MatchType.ARTIST).collect(Collectors.toList());
        this.albumSpecs = this.spec.stream().filter(m -> m.getType() == MatchType.ALBUM).collect(Collectors.toList());
        this.trackSpecs = this.spec.stream().filter(m -> m.getType() == MatchType.TRACK).collect(Collectors.toList());
    }

    public boolean match(Album album) {
        return artistMatch(album.getArtist().getName()) && albumMatch(album.getName());
    }

    public boolean match(Path path) {

        String artist = path.getParent().getParent().getFileName().toString();
        String album = path.getParent().getFileName().toString();
        String track = path.getFileName().toString();

        return artistMatch(artist) && albumMatch(album) && trackMatch(track);
    }

    private boolean trackMatch(String track) {

        return trackSpecs.isEmpty() || trackSpecs.stream().allMatch(m -> m.test(track));
    }

    private boolean albumMatch(String album) {

        return albumSpecs.isEmpty() || albumSpecs.stream().allMatch(m -> m.test(album));
    }

    private boolean artistMatch(String artist) {

        return artistSpecs.isEmpty() || artistSpecs.stream().allMatch(m -> m.test(artist));
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
    }
}

package mediaserver.media;

import mediaserver.hash.AbstractHashable;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PlaylistM3U extends AbstractHashable {

    private final String name;

    private final List<Track> tracks;

    private final Map<Path, Track> locatedTracks;

    private static final long serialVersionUID = -6198219291681770060L;

    public PlaylistM3U(String name, Collection<Track> tracks) {

        this(name, Objects.requireNonNull(tracks, "tracks"), map(tracks));
    }

    public PlaylistM3U(String name, Collection<Track> tracks, Map<Path, Track> locatedTracks) {

        this.name = name;
        this.tracks = List.copyOf(tracks);
        this.locatedTracks = new LinkedHashMap<>(locatedTracks);
    }

    public PlaylistM3U move(Path to, Function<Path, Optional<Path>> dist) {

        return new PlaylistM3U(name, tracks, relocate(to, dist, locatedTracks));
    }

    public String getName() {

        return name;
    }

    public List<Track> getTracks() {

        return tracks;
    }

    @SuppressWarnings("unused")
    public Collection<Entry<String, Track>> getLocatedTracks() {

        return locatedTracks.entrySet().stream().collect(Collectors.toMap(
            e -> e.getKey().toString(),
            Entry::getValue,
            (track1, track2) -> {
                throw new IllegalArgumentException(track1 + " / " + track2);
            },
            LinkedHashMap::new
        )).entrySet();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, name);
        hash(h, tracks);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name).append(" [").append(tracks.size()).append("]");
    }

    private static Map<Path, Track> map(Collection<Track> tracks) {

        return tracks.stream()
            .collect(Collectors.toMap(
                track ->
                    track.getFile().toPath(),
                Function.identity(),
                (track1, track2) -> {
                    throw new IllegalStateException(track1 + " / " + track2);
                },
                LinkedHashMap::new
            ));
    }

    private static Map<Path, Track> relocate(
        Path to,
        Function<Path, Optional<Path>> dist,
        Map<Path, Track> locatedTracks
    ) {

        return locatedTracks.entrySet().stream()
            .collect(Collectors.toMap(
                entry ->
                    to.relativize(dist.apply(entry.getKey()).orElse(entry.getKey())),
                Entry::getValue,
                (track1, track2) -> {
                    throw new IllegalArgumentException(track1 + " / " + track2);
                },
                LinkedHashMap::new));
    }
}

package mediaserver.templates;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import mediaserver.http.Par;

public enum TPar implements Par<Template, Object> {
    media,
    plyr,
    format,
    playlist,
    compressed,
    playlists,
    curation,
    curations,
    mediaCurations,
    mediaSeries,
    mediaPlaylists,
    album,
    artist,
    artists,
    albumArtists,
    series,
    union,
    unionLink,
    selected,
    randomAlbums,
    highlightedAlbum,
    highlightedArtist,
    highlighted,
    trackHighlighted,
    highlightedSelected,
    highlightedRemaining,
    playableGroups,
    albumPlayable,
    admin,
    user,
    host,
    protocol,
    ids,
    sessions,
    streamlease,
    exchanges;

    public static Optional<TPar> get(String substring) {
        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }

    @Override
    public Stream<Object> params(Template template) {
        return Stream.of(template.get(this));
    }
}

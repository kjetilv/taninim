package mediaserver.templates;

import mediaserver.http.Par;

import java.util.Arrays;
import java.util.Optional;

public enum TPar implements Par {

    media,

    plyr,

    playlist,

    playlists,

    curation,

    curations,

    album,

    artist,

    series,

    selected,

    randomAlbums,

    highlightedAlbum,

    highlighted,

    trackHighlighted,

    highlightedSelected,

    highlightedRemaining,

    playableGroups,

    admin,

    user,

    host,

    protocol,

    ids,

    sessions,

    streamlease,

    exchanges;

    @Override
    public String getName() {

        return name();
    }

    public static Optional<TPar> get(String substring) {

        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}

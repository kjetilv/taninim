package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

import java.util.*;
import java.util.stream.Collectors;

public final class Playlist extends AbstractNameHashable {

    private final Map<Album, Collection<Track>> tracks;

    private static final long serialVersionUID = -7432999542297837794L;

    public Playlist(String name) {

        this(name, null);
    }

    public Playlist(String name, Map<Album, Collection<Track>> tracks) {

        super(name);
        this.tracks = tracks == null || tracks.isEmpty() ? Collections.emptyMap()
            : new LinkedHashMap<>(tracks);
    }

    public Playlist add(Playlist playlist) {

        Map<Album, Collection<Track>> map = new LinkedHashMap<>(tracks);
        playlist.tracks.keySet().forEach(album ->
            map.put(album, album.getTracks()));
        return new Playlist(getName(), map);
    }

    public Playlist add(Album album) {

        Map<Album, Collection<Track>> map = new LinkedHashMap<>(tracks);
        map.put(album, album.getTracks());
        return new Playlist(getName(), map);
    }

    public Collection<Track> getTracks() {
        return tracks.values().stream()
            .flatMap(albumTracks ->
                albumTracks.stream()
                    .sorted(Comparator.comparing(Track::getTrackNo)))
            .collect(Collectors.toList());
    }

    public boolean contains(Album album) {

        return tracks.containsKey(album);
    }

    public boolean isEmpty() {

        return tracks.isEmpty();
    }
}

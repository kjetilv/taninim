package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Playlist extends AbstractNameHashable {

    private final Map<Album, Collection<Track>> tracks;

    private static final long serialVersionUID = -7432999542297837794L;

    public Playlist(String name) {

        this(name, null);
    }

    public Playlist(String name, Map<Album, Collection<Track>> tracks) {

        super(name);
        this.tracks = tracks == null || tracks.isEmpty() ? Collections.emptyMap()
            : Map.copyOf(tracks);
    }

    public Playlist add(Playlist playlist) {

        HashMap<Album, Collection<Track>> map = new HashMap<>(tracks);
        playlist.tracks.keySet().forEach(album ->
            map.put(album, album.getTracks()));
        return new Playlist(getName(), map);
    }

    public Playlist add(Album album) {

        HashMap<Album, Collection<Track>> map = new HashMap<>(tracks);
        map.put(album, album.getTracks());
        return new Playlist(getName(), map);
    }

    public boolean contains(Album album) {

        return tracks.containsKey(album);
    }

    public boolean isEmpty() {

        return tracks.isEmpty();
    }
}

package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

import java.util.*;
import java.util.stream.Collectors;

public final class Playlist extends AbstractNameHashable {

    private final Map<Album, Collection<Track>> tracks;

    private static final long serialVersionUID = -7432999542297837794L;

    private Playlist(String name) {

        this(name, null);
    }

    private Playlist(String name, Map<Album, Collection<Track>> tracks) {

        super(name);
        this.tracks = tracks == null || tracks.isEmpty()
            ? Collections.emptyMap()
            : new LinkedHashMap<>(tracks);
    }

    public Collection<Track> getTracks() {

        return tracks.values().stream()
            .flatMap(albumTracks ->
                albumTracks.stream()
                    .sorted(Comparator.comparing(Track::getTrackNo)))
            .collect(Collectors.toList());
    }

    public boolean contains(AlbumTrack albumTrack) {

        return contains(albumTrack.getAlbum()) &&
            this.tracks.get(albumTrack.getAlbum()).contains(albumTrack.getTrack());
    }

    public boolean contains(Album album) {

        return tracks.containsKey(album);
    }

    public static Collection<Playlist> playlistsWith(Album... albums) {

        return convert(PlaylistYaml.PLAYLISTS, Arrays.asList(albums));
    }

    public static Collection<Playlist> curationsWith(Album... albums) {

        return convert(PlaylistYaml.CURATED, Arrays.asList(albums));
    }

    static Collection<Playlist> playlistsWith(Collection<Album> albums) {

        return convert(PlaylistYaml.PLAYLISTS, albums);
    }

    static Collection<Playlist> curationsWith(Collection<Album> albums) {

        return convert(PlaylistYaml.CURATED, albums);
    }

    private Playlist add(Playlist playlist) {

        Map<Album, Collection<Track>> map = new LinkedHashMap<>(tracks);
        map.putAll(playlist.tracks);
        return new Playlist(getName(), map);
    }

    private Playlist add(Album album) {

        return new Playlist(getName(), Map.of(
            album, new HashSet<>(album.getTracks())));
    }

    private boolean isEmpty() {

        return tracks.isEmpty();
    }

    private static Collection<Playlist> convert(Collection<PlaylistYaml> playlists, Collection<Album> albums) {

        return playlists.stream()
            .map(playlist -> albums.stream()
                .filter(playlist::contains)
                .reduce(
                    new Playlist(playlist.getPath().toString()),
                    Playlist::add,
                    Playlist::add))
            .filter(playlist ->
                !playlist.isEmpty())
            .collect(Collectors.toList());
    }
}

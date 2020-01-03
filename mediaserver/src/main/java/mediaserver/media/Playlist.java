package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public boolean contains(Track track) {

        return getTracks().stream().anyMatch(track::equals);
    }

    public boolean contains(Album album) {

        return tracks.containsKey(album);
    }

    public boolean isEmpty() {

        return tracks.isEmpty();
    }

    public static Collection<Playlist> playlistsWith(Album... albums) {

        return with(PlaylistYaml.PLAYLISTS, Arrays.asList(albums));
    }

    public static Collection<Playlist> playlistsWith(Stream<Album> albums) {

        return with(PlaylistYaml.PLAYLISTS, albums.collect(Collectors.toList()));
    }

    public static Collection<Playlist> playlistsWith(Collection<Album> albums) {

        return with(PlaylistYaml.PLAYLISTS, albums);
    }

    public static Collection<Playlist> curationsWith(Album... albums) {

        return with(PlaylistYaml.CURATED, Arrays.asList(albums));
    }

    public static Collection<Playlist> curationsWith(Stream<Album> albums) {

        return with(PlaylistYaml.CURATED, albums.collect(Collectors.toList()));
    }

    public static Collection<Playlist> curationsWith(Collection<Album> albums) {

        return with(PlaylistYaml.CURATED, albums);
    }

    private static Collection<Playlist> with(Collection<PlaylistYaml> playlists, Collection<Album> albums) {

        return playlists.stream()
            .map(customCategory ->
                toPlaylistWith(customCategory, albums))
            .filter(playlist ->
                !playlist.isEmpty())
            .collect(Collectors.toList());
    }

    private static Playlist toPlaylistWith(PlaylistYaml customCategory, Collection<Album> albums) {

        return albums.stream()
            .filter(customCategory::contains)
            .reduce(
                playlist(customCategory),
                Playlist::add,
                Playlist::add);
    }

    private static Playlist playlist(PlaylistYaml customCategory) {

        return new Playlist(customCategory.getPath().toString());
    }
}

package mediaserver.media;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import mediaserver.hash.AbstractNameHashable;

public final class Playlist extends AbstractNameHashable {

    public static Collection<Playlist> playlistsWith(AlbumContext... albumContexts) {
        return convert(PlaylistYaml.PLAYLISTS, Arrays.asList(albumContexts));
    }

    public static Collection<Playlist> curationsWith(AlbumContext... albumContexts) {
        return convert(PlaylistYaml.CURATED, Arrays.asList(albumContexts));
    }

    static Collection<Playlist> playlistsWith(Collection<AlbumContext> albumContexts) {
        return convert(PlaylistYaml.PLAYLISTS, albumContexts);
    }

    static Collection<Playlist> curationsWith(Collection<AlbumContext> albumContexts) {
        return convert(PlaylistYaml.CURATED, albumContexts);
    }

    private final Map<AlbumContext, Collection<Track>> tracks;

    private Playlist(String name) {
        this(name, null);
    }

    private Playlist(String name, Map<AlbumContext, Collection<Track>> tracks) {
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
        return this.tracks.entrySet().stream().anyMatch(e ->
                e.getKey().getAlbum().equals(albumTrack.getAlbum()) && e.getValue().contains(albumTrack.getTrack()));
    }

    public boolean contains(AlbumContext albumContext) {
        return tracks.containsKey(albumContext);
    }

    private Playlist add(AlbumContext albumContext) {
        if (tracks.isEmpty()) {
            return new Playlist(getName(), Map.of(albumContext, new HashSet<>(albumContext.getAlbum().getTracks())));
        }
        this.tracks.put(albumContext, new HashSet<>(albumContext.getAlbum().getTracks()));
        return this;
    }

    private Playlist add(Playlist playlist) {
        if (tracks.isEmpty()) {
            return playlist;
        }
        this.tracks.putAll(playlist.tracks);
        return this;
    }

    private boolean isEmpty() {
        return tracks.isEmpty();
    }

    private static final long serialVersionUID = -7432999542297837794L;

    private static Collection<Playlist> convert(Collection<PlaylistYaml> playlists, Collection<AlbumContext> albums) {
        return playlists.stream()
            .map(playlist ->
                albums.stream()
                    .filter(albumContext ->
                        playlist.contains(albumContext.getAlbum()))
                    .reduce(new Playlist(playlist.getPath().toString()), Playlist::add, Playlist::add))
            .filter(playlist ->
                !playlist.isEmpty())
            .collect(Collectors.toList());
    }
}

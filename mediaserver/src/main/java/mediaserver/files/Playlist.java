package mediaserver.files;

import mediaserver.hash.AbstractHashable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Playlist extends AbstractHashable {

    private final String name;

    private final List<Track> tracks;

    public Playlist(String name, Collection<Track> tracks) {
        this.name = name;
        this.tracks = List.copyOf(tracks);
    }

    public Playlist addAlbums(Collection<Album> albums) {
        return new Playlist(name,
            albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList()));
    }

    public Playlist addTracks(Collection<Track> tracks) {
        return new Playlist(name,
            Stream.of(this.tracks, tracks).flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, name);
        hash(h, tracks);
    }

    @Override
    protected Object toStringBody() {
        return super.toStringBody();
    }

    public String toM3U() {

    }
}

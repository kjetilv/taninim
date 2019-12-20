package mediaserver.media;

import mediaserver.hash.AbstractHashable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PlaylistM3U extends AbstractHashable {

    private final String name;

    private final List<Track> tracks;

    private static final long serialVersionUID = -6198219291681770060L;

    public PlaylistM3U(Album album) {

        this(album.getArtist().getName() + ": " + album.getName(), album.getTracks());
    }

    public PlaylistM3U(Collection<Track> tracks) {

        this(null, tracks);
    }

    public PlaylistM3U(String name, Collection<Track> tracks) {

        this.name = name;
        this.tracks = List.copyOf(tracks);
    }

    public String getName() {

        return name;
    }

    public List<Track> getTracks() {

        return tracks;
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
}

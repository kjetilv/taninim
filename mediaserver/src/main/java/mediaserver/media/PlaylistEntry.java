package mediaserver.media;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

public class PlaylistEntry {

    private final Collection<String> spec;

    public PlaylistEntry(String spec) {

        this.spec = Arrays.asList(Objects.requireNonNull(spec, "spec").split("\\s+&&\\s+"));
    }

    public boolean albumMatch(Album album) {

        return albumMatch(album.getName()) || match(album.getArtist().getName(), this::isArtist);
    }

    public boolean albumMatch(String name) {

        return match(name, this::isAlbum);
    }

    public boolean artistMatch(String name) {

        return match(name, this::isArtist);
    }

    public boolean trackMatch(String name) {

        return match(name, this::isTrack);
    }

    private boolean match(String name, Predicate<String> isAlbum) {

        return spec.stream().filter(isAlbum).anyMatch(part ->
            name.toLowerCase().contains(part.toLowerCase()));
    }

    private boolean isArtist(String part) {

        return part.endsWith("/");
    }

    private boolean isAlbum(String part) {

        return !isTrack(part) && !isArtist(part);
    }

    private boolean isTrack(String part) {

        return part.startsWith("/");
    }
}

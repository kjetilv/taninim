package mediaserver.files;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlbumContext {

    private final Album album;

    private final Credits credits;

    private final Collection<String> comments;

    public AlbumContext(Album album) {

        this(Objects.requireNonNull(album, "album"), null, null);
    }

    private AlbumContext(Album album, Credits credits, Collection<String> comments) {

        this.album = album;
        this.credits = credits;
        this.comments = comments;
    }

    public AlbumContext comments(String... comments) {

        Collection<String> allComments = Stream.concat(
            this.comments.stream(),
            Arrays.stream(comments))
            .distinct().collect(Collectors.toList());
        return new AlbumContext(album, credits, allComments);
    }

    public Credits getCredits() {

        return credits;
    }

    public Album getAlbum() {

        return album;
    }

    public Collection<String> getComments() {

        return comments;
    }

    public AlbumContext credit(String type, String name) {
        return new AlbumContext(
            album,
            credits.credit(type, name),
            comments);
    }

    public AlbumContext append(AlbumContext albumContext) {

        if (!album.equals(albumContext.getAlbum())) {

            throw new IllegalArgumentException(
                this + " could not add album context for other album: " + albumContext);
        }

        List<String> allComments = Stream.concat(
            comments.stream(),
            albumContext.comments.stream()
        ).distinct().collect(Collectors.toList());

        return new AlbumContext(album, credits.append(albumContext.getCredits()), allComments);
    }
}

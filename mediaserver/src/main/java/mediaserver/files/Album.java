package mediaserver.files;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.MostlyOnce;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Album extends AbstractHashable
    implements Comparable<Album>, Serializable {

    private final Artist artist;

    private final String name;

    private final Integer parts;

    private final Integer part;

    private final List<Track> tracks;

    private final CategoryPath categoryPath;

    private final AlbumContext context;

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getCategoryPath)
            .thenComparing(Album::getArtist)
            .thenComparing(Album::getName);

    private static final long serialVersionUID = 6861992470450497236L;

    private final Supplier<List<Artist>> artistsSupplier = MostlyOnce.get(this::resolveArtists);

    Album(CategoryPath categoryPath, Artist artist, String name, List<Track> tracks) {

        this(categoryPath, artist, name, tracks, null);
    }

    Album(CategoryPath categoryPath, Artist artist, String name, List<Track> tracks, AlbumContext context) {

        this(
            artist,
            name.replaceAll("_", ":"),
            parts(tracks),
            null,
            tracks,
            categoryPath,
            context);
    }

    private Album(
        Artist artist,
        String name,
        Integer parts,
        Integer part,
        List<Track> tracks,
        CategoryPath categoryPath,
        AlbumContext context
    ) {

        this.artist = artist;
        this.context = context;
        this.name = single(Track::getAlbum, tracks).orElse(name);
        this.parts = parts;
        this.part = part == null ? null : part + 1;
        this.tracks =
            Objects.requireNonNull(tracks, "track").stream().sorted().collect(Collectors.toList());
        this.categoryPath = categoryPath;
    }

    @SuppressWarnings("unused") // StringTemplate
    public List<Album> getAlbumParts() {

        if (parts == null) {
            return Collections.singletonList(this);
        }
        return IntStream.range(0, parts).mapToObj(part ->
            new Album(
                artist,
                name,
                parts,
                part,
                tracks.stream()
                    .filter(track ->
                        track.getPart() == part + 1)
                    .sorted()
                    .collect(Collectors.toList()),
                categoryPath,
                context
            )).collect(Collectors.toList());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Album album) {

        return ALBUM_COMPARATOR.compare(this, album);
    }

    public Artist getArtist() {

        return artist;
    }

    public String getName() {

        return name;
    }

    public AlbumContext getContext() {

        return context;
    }

    public Collection<Artist> getArtists() {

        return artistsSupplier.get();
    }

    public boolean isAdditionalArtists() {

        return getArtists().size() > 1;
    }

    public List<Artist> resolveArtists() {

        return Stream.of(
            Stream.of(getArtist()),
            tracks.stream().map(Track::getArtist),
            tracks.stream().map(Track::getOtherArtist).flatMap(Optional::stream),
            context.getTrackContexts().stream()
                .map(TrackContext::getCredits)
                .map(Credits::getCredits)
                .flatMap(Collection::stream)
                .map(Credit::getName)
                .map(Artist::new))
            .flatMap(s -> s)
            .distinct()
            .collect(Collectors.toList());
    }

    public Integer getParts() {

        return parts;
    }

    public Integer getPart() {

        return part;
    }

    public List<Track> getTracks() {

        return tracks;
    }

    public CategoryPath getCategoryPath() {

        return categoryPath;
    }

    public boolean isBy(Artist artist) {

        return artist == null || this.artist.equals(artist);
    }

    public boolean isIn(CategoryPath category) {

        return categoryPath.startsWith(category);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, tracks);
    }

    @Override
    public String toStringBody() {

        return categoryPath.getPathString() +
            ": " + artist + "/" + name +
            " [" + tracks.size() + "]";
    }

    public boolean hasTracksBy(Artist artist) {

        return tracks.stream()
            .anyMatch(track ->
                artist == null || track.getOtherArtist().filter(artist::equals).isPresent());
    }

    public Album withContext(AlbumContext albumContext) {

        return new Album(
            artist,
            name,
            parts,
            part,
            tracks,
            categoryPath,
            albumContext
        );
    }

    private static Optional<String> single(Function<Track, String> getAlbum, List<Track> tracks) {

        Collection<String> names = tracks.stream().map(getAlbum).collect(Collectors.toSet());
        return names.size() == 1 ? names.stream().findFirst() : Optional.empty();
    }

    private static Integer parts(List<Track> tracks) {

        OptionalInt maxPart = tracks.stream()
            .map(Track::getPart)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max();
        return maxPart.isPresent() ? maxPart.getAsInt() : null;
    }
}

package mediaserver.files;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class Album implements Comparable<Album> {

    private final String artist;

    private final String name;

    private final Integer parts;

    private final Integer part;

    private final List<Track> tracks;

    private final File file;

    private final CategoryPath categoryPath;

    private final UUID uuid;

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getCategoryPath)
            .thenComparing(Album::getArtist)
            .thenComparing(Album::getName);

    public Album(String artist, String name, List<Track> tracks, File file, CategoryPath categoryPath) {
        this(
            artist.replaceAll("_", ":"),
            name.replaceAll("_", ":"),
            parts(tracks),
            null,
            tracks,
            file,
            categoryPath,
            UUID.randomUUID());
    }

    private Album(
        String artist,
        String name,
        Integer parts,
        Integer part,
        List<Track> tracks,
        File file,
        CategoryPath categoryPath,
        UUID uuid
    ) {
        this.artist = artist;
        this.name = single(Track::getAlbum, tracks).orElse(name);
        this.parts = parts;
        this.part = part == null ? null : part + 1;
        this.tracks =
            Objects.requireNonNull(tracks, "track").stream().sorted().collect(Collectors.toList());
        this.file = file;
        this.categoryPath = categoryPath;
        this.uuid = uuid;
    }

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
                file,
                categoryPath,
                uuid
            )).collect(Collectors.toList());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Album album) {
        return ALBUM_COMPARATOR.compare(this, album);
    }

    public String getArtist() {
        return artist;
    }

    public String getName() {
        return name;
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

    public File getFile() {
        return file;
    }

    public CategoryPath getCategoryPath() {
        return categoryPath;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String print() {
        return categoryPath + "/ " + getArtist() + ": " + getName() + " [" + tracks.size() + "]";
    }

    public boolean isIn(CategoryPath category) {
        return categoryPath.startsWith(category);
    }

    public Optional<Track> nextTrack(Track track) {
        return tracks.stream().filter(next ->
            next.getTrackNo() == track.getTrackNo() + 1 ||
                parts != null && next.getTrackNo() == 1 && next.getPart() == track.getPart() + 1
        ).findFirst();
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

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + categoryPath.getPathString() +
            ": " + artist + "/" + name +
            " [" + tracks.size() +
            "]]";
    }
}

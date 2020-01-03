package mediaserver.media;

import mediaserver.hash.AbstractHashable;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LocalMedia extends AbstractHashable implements Media, Serializable {

    public static final Random RND = new Random();

    private final CategoryPath categoryPath;

    private final Collection<Album> albums;

    private final Map<Album, AlbumContext> albumContexts;

    private final Collection<Artist> artists;

    private final Collection<CategoryPath> categories;

    private static final long serialVersionUID = -7165763549356996140L;

    private Collection<Playlist> playlists;

    private final Collection<Playlist> curations;

    public LocalMedia(Path root) {

        this(null, getAlbums(root, root), null);
    }

    private LocalMedia(
        CategoryPath categoryPath,
        Stream<Album> albums,
        Map<Album, AlbumContext> albumContexts
    ) {

        this.categoryPath = categoryPath == null
            ? new CategoryPath()
            : categoryPath;
        this.albums = albums.collect(Collectors.toList());
        this.categories = this.albums.stream()
            .map(Album::getCategoryPath)
            .flatMap(CategoryPath::subPaths)
            .collect(Collectors.toSet());
        this.albumContexts = albumContexts == null || albumContexts.isEmpty()
            ? Collections.emptyMap()
            : Map.copyOf(albumContexts);
        Collection<Album> mediaAlbums = albumStream(true).collect(Collectors.toList());
        this.artists = Stream.concat(
            mediaAlbums.stream().map(Album::getArtist),
            trackStream(true).flatMap(track ->
                Stream.concat(
                    Stream.of(track.getArtist()),
                    track.getOtherArtists().stream()
                )))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        this.playlists =
            Playlist.playlistsWith(mediaAlbums);
        this.curations =
            Playlist.curationsWith(mediaAlbums);
    }

    @Override
    public Media subLibrary(CategoryPath categoryPath, Artist artist, Series series, Playlist playlist) {

        CategoryPath sub = categoryPath == null
            ? this.categoryPath
            : this.categoryPath.sub(categoryPath);
        Predicate<Album> categorized = album ->
            album.isIn(sub);
        Predicate<Album> forArtist = album ->
            artist == null || album.isBy(artist) || album.hasTracksBy(artist) || album.features(artist);
        Predicate<Album> inSeries = series == null
            ? album -> true
            : album -> album.getSeries().contains(series);
        Predicate<Album> inPlaylist = playlist == null
            ? album -> true
            : playlist::contains;

        return new LocalMedia(
            sub,
            stream(true).filter(categorized.and(forArtist).and(inSeries).and(inPlaylist)),
            albumContexts);
    }

    @Override
    public Media withAlbumContext(UUID albumId, AlbumContext albumContext) {

        return getAlbum(albumId).map(album -> {

            Map<Album, AlbumContext> copy = new HashMap<>(albumContexts);
            AlbumContext expandedContext = copy.computeIfAbsent(album, AlbumContext::new)
                .append(albumContext);
            Album expanded = album.withContext(albumContext);
            copy.put(expanded, expandedContext);
            return new LocalMedia(
                categoryPath,
                albums.stream().map(a ->
                    a.equals(album) ? expanded : a),
                copy);
        }).orElseThrow(() ->
            new IllegalArgumentException("Unknown album: " + albumId));
    }

    @Override
    public Optional<Track> getTrack(UUID uuid) {

        return trackStream(true)
            .filter(track ->
                track.getUuid().equals(uuid)).findFirst();
    }

    @Override
    public CategoryPath getCategoryPath() {

        return categoryPath;
    }

    @Override
    public Optional<CategoryPath> getCategoryPath(UUID uuid) {

        return getCategories().stream().filter(category -> category.getUuid().equals(uuid)).findFirst();
    }

    @Override
    public Collection<CategoryPath> getTopCategories() {

        return categoryStream()
            .filter(cat ->
                !cat.equals(this.categoryPath))
            .map(cat ->
                cat.toTop(this.categoryPath))
            .flatMap(Optional::stream)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<CategoryPath> getCategories() {

        return categoryStream()
            .distinct()
            .map(CategoryPath::toRoot)
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Playlist> getPlaylists() {

        return playlists;
    }

    @Override
    public Optional<Playlist> getPlaylist(UUID uuid) {

        return getPlaylist(this.playlists, uuid);
    }

    @Override
    public Collection<Playlist> getCurations() {

        return curations;
    }

    @Override
    public boolean isCurated(Track track) {

        return getCurations().stream().anyMatch(playlist -> playlist.contains(track));
    }

    @Override
    public Optional<Playlist> getCuration(UUID uuid) {

        return getPlaylist(this.curations, uuid);
    }

    @Override
    public Duration getDuration() {

        return albumStream(true).map(Album::getDuration).reduce(
            Duration.ZERO,
            Duration::plus);
    }

    @Override
    public Collection<Artist> getAlbumArtists(boolean recurse) {

        return stream(recurse).map(Album::getArtists).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public Collection<Artist> getArtists(boolean recurse) {

        return artists;
    }

    @Override
    public Collection<Artist> getAlbumCreditedArtists() {

        return collectArtists(Album::getArtists);
    }

    @Override
    public Collection<Artist> getTrackCreditedArtists() {

        return collectArtists(Album::getAllArtists);
    }

    @Override
    public Collection<Album> getRandomAlbums(int count) {

        List<Album> indexed = this.albums.stream()
            .filter(album ->
                album.getContext().getDiscogCover() != null)
            .collect(Collectors.toCollection(ArrayList::new));

        if (count < indexed.size()) {
            Collection<Integer> indexes = new HashSet<>();
            while (indexes.size() < count) {
                indexes.add(RND.nextInt(indexed.size()));
            }
            return indexes.stream().map(indexed::get).collect(Collectors.toList());
        }
        Collections.shuffle(indexed);
        return indexed;
    }

    @Override
    public Collection<Album> getAlbums(boolean recurse) {

        return albumStream(recurse).collect(Collectors.toList());
    }

    @Override
    public Collection<Track> getTracksFeaturing(Artist artist) {

        Objects.requireNonNull(artist, "artist");
        return albumStream(true)
            .map(Album::getTracks)
            .flatMap(Collection::stream)
            .filter(track ->
                artist.equals(track.getArtist()) ||
                    track.getArtists().stream().anyMatch(artist::equals) ||
                    track.getOtherArtists().stream().anyMatch(artist::equals))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Track> getTracks(boolean recurse) {

        return trackStream(recurse).collect(Collectors.toList());
    }

    @Override
    public Optional<Album> getAlbum(UUID uuid) {

        return stream(true).filter(album -> album.getUuid().equals(uuid)).findFirst();
    }

    @Override
    public Collection<Series> getSeries() {

        return albumStream(true)
            .map(Album::getSeries)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbumsFeaturing(Artist artist) {

        return albumStream(true).filter(album ->
            album.getAllArtists().contains(artist)
        ).collect(Collectors.toList());
    }

    @Override
    public Optional<Artist> getArtist(UUID id) {

        return getTrackCreditedArtists().stream().filter(artist -> artist.getUuid().equals(id)).findFirst();
    }

    @Override
    public Optional<Series> getSeries(UUID id) {

        return getSeries().stream().filter(series -> series.getUuid().equals(id)).findFirst();
    }

    @Override
    public Optional<Artist> getArtist(String name) {

        return getArtists(true).stream()
            .filter(artist -> artist.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public Optional<Album> getAlbum(String albumName) {

        return getAlbums(true).stream()
            .filter(album ->
                album.getName().equalsIgnoreCase(albumName))
            .findFirst();
    }

    @Override
    public boolean isEmpty() {

        return albums.isEmpty();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, getAlbums(true));
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(albums.size()).append(" albums");
    }

    private Optional<Playlist> getPlaylist(Collection<Playlist> playlists, UUID uuid) {

        return playlists.stream().filter(playlist -> playlist.getUuid().equals(uuid)).findFirst();
    }

    private Collection<Artist> collectArtists(Function<Album, Collection<Artist>> getAllArtists) {

        return albumStream(true)
            .map(getAllArtists)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private Stream<Album> stream(boolean recurse) {

        return albums.stream().filter(album -> recurse || isInCategory(album));
    }

    private boolean isInCategory(Album album) {

        return album.getCategoryPath().startsWith(this.categoryPath);
    }

    private Stream<CategoryPath> categoryStream() {

        return stream(true).map(Album::getCategoryPath);
    }

    private Stream<Album> albumStream(boolean recurse) {

        return stream(recurse).sorted();
    }

    private Stream<Track> trackStream(boolean recurse) {

        return albumStream(recurse).flatMap(album -> album.getTracks().stream());
    }

    private static Album album(Path root, Path path, Artist artist, String name, List<Track> tracks) {

        CategoryPath categoryPath = new CategoryPath(root.relativize(path.getParent().getParent()));
        return new Album(categoryPath, artist, name, tracks);
    }

    private static Stream<Album> getAlbums(Path root, Path path) {

        if (root == null) {
            return Stream.empty();
        }
        return Stream.concat(
            album(root, path).stream(),
            subDirs(path.toFile())
                .flatMap(subDir ->
                    getAlbums(root, path.resolve(subDir.getName()))));
    }

    private static Optional<Album> album(Path root, Path path) {

        File dir = path.toFile();
        return trackFiles(dir).map(tracks ->
            album(
                root,
                path,
                artist(dir),
                albumName(dir),
                tracks(tracks)));
    }

    private static List<Track> tracks(Collection<File> tracks) {

        return tracks.stream().map(Track::new).collect(Collectors.toList());
    }

    private static Stream<File> subDirs(File dir) {

        return Optional.ofNullable(
            dir.listFiles(file ->
                file.isDirectory() && !file.getName().equals("objects")))
            .stream()
            .flatMap(Arrays::stream);
    }

    private static Optional<List<File>> trackFiles(File dir) {

        return Optional.ofNullable(
            dir.listFiles(file ->
                file.isFile() && file.getName().endsWith(".flac")))
            .filter(files ->
                files.length > 0)
            .map(Arrays::asList);
    }

    private static String albumName(File dir) {

        return dir.getName();
    }

    private static Artist artist(File dir) {

        return Artist.get(dir.getParentFile().getName().replaceAll("_", ":"));
    }
}

package mediaserver.media;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Year;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.hash.AbstractHashable;

public final class LocalMedia
    extends AbstractHashable
    implements Media, Serializable {

    private final Collection<Album> albums;

    private final Comparator<Album> albumComparator;

    private final Map<Album, AlbumContext> albumContexts;

    private final Collection<Artist> artists;

    private final Collection<Playlist> playlists;

    private final Collection<Playlist> curations;

    private final Map<UUID, Boolean> curatedTracks = new ConcurrentHashMap<>();

    private final Map<UUID, Album> trackIndex;

    LocalMedia(Path root) {
        this(getAlbums(root, root), null);
    }

    private LocalMedia(Stream<Album> albums, Map<Album, AlbumContext> albumContexts) {
        this(
            Objects.requireNonNull(albums, "albums").collect(Collectors.toList()),
            albumContexts,
            null);
    }

    private LocalMedia(
        Collection<Album> albums,
        Map<Album, AlbumContext> albumContexts,
        Comparator<Album> albumComparator
    ) {
        this.albums = List.copyOf(albums);
        this.albumComparator = albumComparator;
        this.trackIndex = this.albums.stream().flatMap(album ->
            album.getTracks().stream().map(track ->
                new AbstractMap.SimpleEntry<>(
                    track.getUuid(),
                    album)))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        this.albumContexts = albumContexts == null || albumContexts.isEmpty()
            ? Collections.emptyMap()
            : Map.copyOf(albumContexts);
        Collection<Album> mediaAlbums = albumStream().collect(Collectors.toList());
        this.artists = Stream.concat(
            mediaAlbums.stream().map(Album::getArtist),
            trackStream().flatMap(track ->
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
    public Media subLibrary(
        Collection<Artist> artists,
        Collection<Series> series,
        Collection<Playlist> playlists,
        Collection<Playlist> curations,
        boolean union
    ) {
        Optional<Predicate<Album>> forArtist = Optional.ofNullable(artists)
            .filter(a -> !a.isEmpty())
            .map(a ->
                album ->
                    filtered(a, artistMatch(album), union));
        Optional<Predicate<Album>> inSeries = Optional.ofNullable(series)
            .filter(s -> !s.isEmpty())
            .map(s ->
                album ->
                    filtered(s, seriesMatch(album), union));
        Optional<Predicate<Album>> inPlaylist = Optional.ofNullable(playlists)
            .filter(p ->
                !p.isEmpty())
            .map(p ->
                album ->
                    filtered(p, playlistMatch(album), union));
        Optional<Predicate<Album>> curated = Optional.ofNullable(curations)
            .filter(c ->
                !c.isEmpty())
            .map(c ->
                album ->
                    filtered(c, playlistMatch(album), union));
        Optional<Predicate<Album>> filter =
            Stream.of(forArtist, inSeries, inPlaylist, curated).flatMap(Optional::stream)
                .reduce(union ? Predicate::or : Predicate::and);
        return new LocalMedia(
            albums.stream().filter(filter.orElse(p -> true)).collect(Collectors.toList()),
            albumContexts,
            albumComparator);
    }

    @Override
    public Media sortedAlbums(Comparator<Album> comparator) {
        return new LocalMedia(albums, albumContexts, comparator);
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
                albums.stream()
                    .map(a ->
                        a.equals(album) ? expanded : a)
                    .collect(Collectors.toList()),
                copy,
                albumComparator);
        }).findFirst().orElseThrow(() ->
            new IllegalArgumentException("Unknown album: " + albumId));
    }

    @Override
    public Stream<AlbumTrack> getAlbumTrack(UUID uuid) {
        return Optional.ofNullable(trackIndex.get(uuid))
            .flatMap(album ->
                album.getTrack(uuid).map(track ->
                    new AlbumTrack(album, track)))
            .stream();
    }

    @Override
    public Stream<Track> getTrack(UUID uuid) {
        return trackStream()
            .filter(track ->
                track.getUuid().equals(uuid));
    }

    @Override
    public Year getStartYear() {
        return albumStream()
            .map(album -> album.getContext().getYear())
            .filter(Objects::nonNull)
            .min(Year::compareTo)
            .orElse(null);
    }

    @Override
    public Year getEndYear() {
        return albumStream()
            .map(album -> album.getContext().getYear())
            .filter(Objects::nonNull)
            .max(Year::compareTo)
            .orElse(null);
    }

    @Override
    public Collection<Playlist> getPlaylists() {
        return Collections.unmodifiableCollection(playlists);
    }

    @Override
    public Stream<Playlist> getPlaylist(UUID uuid) {
        return getPlaylist(this.playlists, uuid);
    }

    @Override
    public Collection<Playlist> getCurations() {
        return Collections.unmodifiableCollection(curations);
    }

    @Override
    public Stream<Playlist> getCuration(UUID uuid) {
        return getPlaylist(this.curations, uuid);
    }

    @Override
    public boolean isCurated(AlbumTrack albumTrack) {
        return curatedTracks.computeIfAbsent(
            albumTrack.getTrack().getUuid(),
            uuid ->
                getCurations().stream().anyMatch(curation ->
                    curation.contains(albumTrack)));
    }

    @Override
    public Duration getDuration() {
        return albumStream().map(Album::getDuration).reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public Collection<Artist> getAlbumArtists(boolean recurse) {
        return albums.stream().map(Album::getArtists).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public Collection<Artist> getArtists(boolean recurse) {
        return Collections.unmodifiableCollection(artists);
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
        if (count >= indexed.size()) {
            Collections.shuffle(indexed);
            return indexed;
        }
        Collection<Integer> randomIndexes = new HashSet<>();
        while (randomIndexes.size() < count) {
            randomIndexes.add(RND.nextInt(indexed.size()));
        }
        return randomIndexes.stream().map(indexed::get).collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbums() {
        return albumStream().collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbumsByYear() {
        return albumStream()
            .sorted(Comparator.comparing(
                album -> Optional.ofNullable(album.getContext()).map(AlbumContext::getYear).orElse(Year.now())))
            .collect(Collectors.toList());
    }

    @Override
    public Stream<Track> getTracksFeaturing(Artist artist) {
        Objects.requireNonNull(artist, "artist");
        return albumStream()
            .map(Album::getTracks)
            .flatMap(Collection::stream)
            .filter(track ->
                artist.equals(track.getArtist()) ||
                    track.getArtists().stream().anyMatch(artist::equals) ||
                    track.getOtherArtists().stream().anyMatch(artist::equals));
    }

    @Override
    public Collection<Track> getTracks(boolean recurse) {
        return trackStream().collect(Collectors.toList());
    }

    @Override
    public Stream<Album> getAlbum(UUID uuid) {
        return albums.stream().filter(album -> album.getUuid().equals(uuid));
    }

    @Override
    public Collection<Series> getSeries() {
        return albumStream()
            .map(Album::getSeries)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbumsFeaturing(Artist artist) {
        return albumStream().filter(album ->
            album.getAllArtists().contains(artist)
        ).collect(Collectors.toList());
    }

    @Override
    public Stream<Artist> getArtist(UUID id) {
        return getTrackCreditedArtists().stream().filter(artist -> artist.getUuid().equals(id));
    }

    @Override
    public Stream<Series> getSeries(UUID id) {
        return getSeries().stream().filter(series -> series.getUuid().equals(id));
    }

    @Override
    public Stream<Artist> getArtist(String name) {
        return getArtists(true).stream()
            .filter(artist -> artist.getName().equalsIgnoreCase(name));
    }

    @Override
    public Stream<Album> getAlbum(String albumName) {
        return getAlbums().stream()
            .filter(album ->
                album.getName().equalsIgnoreCase(albumName));
    }

    @Override
    public boolean isEmpty() {
        return albums.isEmpty();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, getAlbums());
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append(albums.size()).append(" albums");
    }

    private Collection<Artist> collectArtists(Function<? super Album, ? extends Collection<Artist>> getAllArtists) {
        return albumStream()
            .map(getAllArtists)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private Stream<Album> albumStream() {
        Comparator<Album> comparator = albumComparator == null
            ? Comparator.naturalOrder()
            : albumComparator;
        return albums.stream().sorted(comparator);
    }

    private Stream<Track> trackStream() {
        return albumStream().flatMap(album -> album.getTracks().stream());
    }

    private static final Random RND = new Random();

    private static final long serialVersionUID = -7165763549356996140L;

    private static final Pattern UNDERSCORE = Pattern.compile("_");

    private static Predicate<Playlist> playlistMatch(Album album) {
        return playlist -> playlist.contains(album);
    }

    private static Predicate<Artist> artistMatch(Album album) {
        return artist -> album.isBy(artist) || album.hasTracksBy(artist) || album.features(artist);
    }

    private static <T> boolean filtered(Collection<? extends T> ts, Predicate<? super T> predicate, boolean union) {
        return union
            ? ts.stream().anyMatch(predicate)
            : ts.stream().allMatch(predicate);
    }

    private static Predicate<Series> seriesMatch(Album album) {
        return album.getSeries()::contains;
    }

    private static Stream<Playlist> getPlaylist(Collection<Playlist> playlists, UUID uuid) {
        return playlists.stream()
            .filter(playlist ->
                playlist.getUuid().equals(uuid));
    }

    private static Album album(Artist artist, String name, List<Track> tracks) {
        return new Album(artist, name, tracks);
    }

    private static Stream<Album> getAlbums(Path root, Path path) {
        if (root == null) {
            return Stream.empty();
        }
        return Stream.concat(
            album(path).stream(),
            subDirs(path.toFile())
                .flatMap(subDir ->
                    getAlbums(root, path.resolve(subDir.getName()))));
    }

    private static Optional<Album> album(Path path) {
        File dir = path.toFile();
        return trackFiles(dir).map(tracks ->
            album(
                artist(dir),
                albumName(dir),
                tracks(tracks)));
    }

    private static List<Track> tracks(Collection<? extends File> tracks) {
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
        return Artist.get(UNDERSCORE.matcher(dir.getParentFile().getName()).replaceAll(":"));
    }
}

package mediaserver.media;

import java.io.File;
import java.io.Serial;
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

    private final Comparator<AlbumContext> albumComparator;

    private final Map<UUID, AlbumContext> albumContexts;

    private final Collection<Artist> artists;

    private final Collection<Playlist> playlists;

    private final Collection<Playlist> curations;

    private final Map<UUID, Boolean> curatedTracks = new ConcurrentHashMap<>();

    private final Map<UUID, AlbumContext> trackIndex;

    LocalMedia(Path root) {
        this(getAlbumContexts(root, root).collect(Collectors.toList()));
    }

    private LocalMedia(Collection<AlbumContext> albumContexts) {
        this(albumContexts, null);
    }

    private LocalMedia(Collection<AlbumContext> albumContexts, Comparator<AlbumContext> albumComparator) {
        this.albumContexts = albumContexts == null || albumContexts.isEmpty()
            ? Collections.emptyMap()
            : albumContexts.stream().collect(Collectors.toMap(
                albumContext ->
                    albumContext.getAlbum().getUuid(),
                Function.identity()));
        this.albumComparator = albumComparator == null
            ? Comparator.naturalOrder()
            : albumComparator;
        this.trackIndex = this.albumContexts.values().stream()
            .flatMap(albumContext ->
                albumContext.getAlbum().getTracks().stream().map(track ->
                    new AbstractMap.SimpleEntry<>(
                        track.getUuid(),
                        albumContext)))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
        Collection<AlbumContext> albumContextCollection = albumStream().collect(Collectors.toList());
        this.artists = Stream.concat(
            albumContextCollection.stream()
                .map(AlbumContext::getAlbum)
                .map(Album::getArtist),
            trackStream().flatMap(track ->
                Stream.concat(
                    Stream.of(track.getArtist()),
                    track.getOtherArtists().stream()
                )))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        this.playlists =
            Playlist.playlistsWith(albumContextCollection);
        this.curations =
            Playlist.curationsWith(albumContextCollection);
    }

    @Override
    public Media subLibrary(
        Collection<Artist> artists,
        Collection<Series> series,
        Collection<Playlist> playlists,
        Collection<Playlist> curations,
        boolean union
    ) {
        Optional<Predicate<AlbumContext>> forArtist = Optional.ofNullable(artists)
            .filter(a -> !a.isEmpty())
            .map(a ->
                ctx ->
                    filtered(a, artistMatch(ctx), union));
        Optional<Predicate<AlbumContext>> inSeries = Optional.ofNullable(series)
            .filter(s -> !s.isEmpty())
            .map(s ->
                ctx ->
                    filtered(s, seriesMatch(ctx), union));
        Optional<Predicate<AlbumContext>> inPlaylist = Optional.ofNullable(playlists)
            .filter(p ->
                !p.isEmpty())
            .map(p ->
                ctx ->
                    filtered(p, playlistMatch(ctx), union));
        Optional<Predicate<AlbumContext>> curated = Optional.ofNullable(curations)
            .filter(c ->
                !c.isEmpty())
            .map(c ->
                ctx ->
                    filtered(c, playlistMatch(ctx), union));
        Optional<Predicate<AlbumContext>> filter =
            Stream.of(forArtist, inSeries, inPlaylist, curated)
                .flatMap(Optional::stream)
                .reduce(union ? Predicate::or : Predicate::and);
        return new LocalMedia(
            albumContexts.values().stream().filter(filter.orElse(p -> true))
                .collect(Collectors.toList()),
            albumComparator);
    }

    @Override
    public Media sortedAlbums(Comparator<AlbumContext> comparator) {
        return new LocalMedia(albumContexts.values(), comparator);
    }

    @Override
    public Media withAlbumContexts(Collection<AlbumContext> albumContexts) {
        return new LocalMedia(albumContexts, albumComparator);
    }

    @Override
    public Stream<AlbumTrack> getAlbumTrack(UUID uuid) {
        return Optional.ofNullable(trackIndex.get(uuid))
            .flatMap(albumContext ->
                albumContext.getAlbum().getTrack(uuid)
                    .map(track ->
                        new AlbumTrack(albumContext, track)))
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
            .map(AlbumContext::getYear)
            .filter(Objects::nonNull)
            .min(Year::compareTo)
            .orElse(null);
    }

    @Override
    public Year getEndYear() {
        return albumStream()
            .map(AlbumContext::getYear)
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
        return albumStream()
            .map(AlbumContext::getAlbum)
            .map(Album::getDuration)
            .reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public Collection<Artist> getAlbumArtists(boolean recurse) {
        return albumContexts.values().stream()
            .map(AlbumContext::getAllArtists)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Artist> getArtists(boolean recurse) {
        return Collections.unmodifiableCollection(artists);
    }

    @Override
    public Collection<Artist> getTrackCreditedArtists() {
        return collectArtists(AlbumContext::getAllArtists);
    }

    @Override
    public Collection<AlbumContext> getRandomAlbums(int count) {
        List<AlbumContext> indexed = this.albumContexts.values().stream()
            .filter(album ->
                album.getDiscogCover() != null)
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
    public Collection<AlbumContext> getAlbumContexts() {
        return albumContexts.values().stream()
            .filter(Objects::nonNull)
            .sorted(albumComparator)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Album> getAlbums() {
        return albumStream().map(AlbumContext::getAlbum).collect(Collectors.toList());
    }

    @Override
    public int getAlbumCount() {
        return albumContexts.size();
    }

    @Override
    public List<AlbumContext> getAlbumsByYear() {
        return albumStream()
            .sorted(Comparator.comparing(
                album -> Optional.ofNullable(album.getYear()).orElseGet(Year::now)))
            .collect(Collectors.toList());
    }

    @Override
    public Stream<Track> getTracksFeaturing(Artist artist) {
        Objects.requireNonNull(artist, "artist");
        return albumStream()
            .map(AlbumContext::getAlbum)
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
        return Optional.ofNullable(albumContexts.get(uuid)).stream().map(AlbumContext::getAlbum);
    }

    @Override
    public Collection<Series> getSeries() {
        return albumStream()
            .map(AlbumContext::getSeries)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<AlbumContext> getAlbumsFeaturing(Artist artist) {
        return albumStream()
            .filter(albumContext ->
                albumContext.getAllArtists().contains(artist))
            .collect(Collectors.toList());
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
        return albumContexts.isEmpty();
    }

    @Override
    public Stream<AlbumContext> getAlbumContext(UUID uuid) {
        return Optional.ofNullable(albumContexts.get(uuid)).stream();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, getAlbums());
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append(albumContexts.size()).append(" albums");
    }

    private Collection<Artist> collectArtists(Function<? super AlbumContext, ? extends Collection<Artist>> getAllArtists) {
        return albumStream()
            .map(getAllArtists)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private Stream<AlbumContext> albumStream() {
        Comparator<AlbumContext> comparator = albumComparator == null
            ? Comparator.naturalOrder()
            : albumComparator;
        return albumContexts.values().stream().sorted(comparator);
    }

    private Stream<Track> trackStream() {
        return albumStream()
            .map(AlbumContext::getAlbum)
            .flatMap(album ->
                album.getTracks().stream());
    }

    private static final Random RND = new Random();

    @Serial private static final long serialVersionUID = -7165763549356996140L;

    private static final Pattern UNDERSCORE = Pattern.compile("_");

    private static Predicate<Playlist> playlistMatch(AlbumContext albumContext) {
        return playlist ->
            playlist.contains(albumContext);
    }

    private static Predicate<Artist> artistMatch(AlbumContext albumContext) {
        return artist -> albumContext.getAlbum().isBy(artist) ||
            albumContext.getAlbum().hasTracksBy(artist) ||
            albumContext.features(artist);
    }

    private static <T> boolean filtered(Collection<? extends T> ts, Predicate<? super T> predicate, boolean union) {
        return union
            ? ts.stream().anyMatch(predicate)
            : ts.stream().allMatch(predicate);
    }

    private static Predicate<Series> seriesMatch(AlbumContext album) {
        return album.getSeries()::contains;
    }

    private static Stream<Playlist> getPlaylist(Collection<Playlist> playlists, UUID uuid) {
        return playlists.stream()
            .filter(playlist ->
                playlist.getUuid().equals(uuid));
    }

    private static AlbumContext albumContext(Artist artist, String name, List<Track> tracks) {
        return new AlbumContext(new Album(artist, name, tracks));
    }

    private static Stream<AlbumContext> getAlbumContexts(Path root, Path path) {
        if (root == null) {
            return Stream.empty();
        }
        return Stream.concat(
            albumContext(path).stream(),
            subDirs(path.toFile())
                .flatMap(subDir ->
                    getAlbumContexts(root, path.resolve(subDir.getName()))));
    }

    private static Optional<AlbumContext> albumContext(Path path) {
        File dir = path.toFile();
        return trackFiles(dir).map(tracks ->
            albumContext(
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

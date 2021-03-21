package mediaserver.media;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.externals.DiscogConnection;
import mediaserver.externals.DiscogImage;
import mediaserver.externals.DiscogReleaseDigest;
import mediaserver.externals.DiscogSeriesDigest;
import mediaserver.externals.DiscogTrackDigest;
import mediaserver.externals.DiscogVideo;
import mediaserver.externals.DiscogsDataResolver;
import mediaserver.externals.IOSMapParser;
import mediaserver.externals.iTunesLibrary;
import mediaserver.util.DAC;
import mediaserver.util.IO;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

    Duration FORTNITE = Duration.ofDays(14);

    static Media local(Path mediaPath, Path libraryPath, Path resourcesPath) {
        Media baseMedia = scanMedia(mediaPath);
        iTunesLibrary iTunesLibrary = scanLibrary(libraryPath);
        DiscogsDataResolver discogsData = scanDiscogsData(resourcesPath, baseMedia, iTunesLibrary);
        return combineMedia(baseMedia, discogsData);
    }

    static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("NO combine");
        };
    }

    static Long id(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
        } catch (Exception e) {
            throw new IllegalStateException("Bad release: " + uri, e);
        }
    }

    static Credits trackCredits(DiscogTrackDigest track) {
        return Optional.ofNullable(track.getExtraartists()).stream().flatMap(
            Collection::stream).reduce(
            new Credits(),
            (credits, dad) ->
                credits.credit(dad.getName(), dad.getUri(), dad.getRole()),
            noCombine());
    }

    static List<Video> videos(DiscogReleaseDigest release) {
        return Optional.ofNullable(release.getVideos()).stream().flatMap(Collection::stream)
            .flatMap(Media::video)
            .collect(Collectors.toList());
    }

    static Stream<Video> video(DiscogVideo discogVideo) {
        try {
            return Stream.of(
                new Video(discogVideo.getTitle(), discogVideo.getDescription(), discogVideo.getUri()));
        } catch (Exception e) {
            log.warn("Bad video: {}", discogVideo, e);
            return Stream.empty();
        }
    }

    static Optional<URI> cover150(DiscogReleaseDigest release) {
        return getCover(release, DiscogImage::getUri150);
    }

    static Optional<URI> cover(DiscogReleaseDigest release) {
        return getCover(release, DiscogImage::getUri);
    }

    static Optional<URI> getCover(DiscogReleaseDigest release, Function<? super DiscogImage, URI> toUri) {
        return Stream.concat(
            release.getImages().stream().filter(Media::isPrimary),
            release.getImages().stream().filter(Media::hasImage)
        ).filter(Media::hasImage)
            .findFirst()
            .map(toUri);
    }

    static List<Series> series(DiscogReleaseDigest release) {
        return release.getSeries().stream()
            .map(DiscogSeriesDigest::getName)
            .map(Series::get)
            .collect(Collectors.toList());
    }

    static Year yearOf(DiscogReleaseDigest release) {
        return Optional.ofNullable(release.getYear()).map(Year::parse).orElse(null);
    }

    static Collection<DiscogConnection> metaConnections(Media media, iTunesLibrary iTunesLibrary) {
        return iTunesLibrary.getTracks().values().stream()
            .filter(track ->
                Optional.ofNullable(track.getComments()).filter(comments ->
                    comments.contains("discogs"))
                    .isPresent())
            .flatMap(track ->
                media.getAlbum(track.getAlbum())
                    .map(album ->
                        new DiscogConnection(
                            album,
                            track,
                            URI.create(track.getComments().trim()))))
            .distinct()
            .collect(Collectors.toList());
    }

    static iTunesLibrary iTunesLibrary(Path libraryPath) {
        try {
            Map<String, ?> plist = IO.readFromStream(libraryPath, IOSMapParser::convert);
            return IO.OM.readerFor(iTunesLibrary.class)
                .readValue(IO.OM.writerFor(Map.class)
                    .writeValueAsBytes(plist));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read iTunes library @ " + libraryPath, e);
        }
    }

    static Media empty() {
        return new LocalMedia(null);
    }

    enum AlbumSort {
        ARTIST, TITLE, YEAR
    }

    default Media subLibrary(Series series) {
        return subLibrary(null, Collections.singleton(series), null, null, false);
    }

    default Media subLibrary(Playlist playlist) {
        return subLibrary(null, null, Collections.singletonList(playlist), null, false);
    }

    Media subLibrary(
        Collection<Artist> artist,
        Collection<Series> series,
        Collection<Playlist> playlist,
        Collection<Playlist> curations,
        boolean union
    );

    Media sortedAlbums(Comparator<AlbumContext> comparator);

    Media withAlbumContexts(Collection<AlbumContext> albumContexts);

    Stream<AlbumTrack> getAlbumTrack(UUID uuid);

    Stream<Track> getTrack(UUID uuid);

    @DAC
    Year getStartYear();

    @DAC
    Year getEndYear();

    Collection<Playlist> getPlaylists();

    Stream<Playlist> getPlaylist(UUID uuid);

    Collection<Playlist> getCurations();

    Stream<Playlist> getCuration(UUID uuid);

    boolean isCurated(AlbumTrack albumTrack);

    Duration getDuration();

    @DAC
    default String getPrettyDuration() {
        return Print.prettyLongTime(getDuration());
    }

    default Collection<Artist> getAllAlbumArtists() {
        return getAlbumArtists(true);
    }

    Collection<Artist> getAlbumArtists(boolean recurse);

    default Collection<Artist> getArtists() {
        return getArtists(false);
    }

    Collection<Artist> getArtists(boolean recurse);

    Collection<Artist> getTrackCreditedArtists();

    Collection<AlbumContext> getRandomAlbums(int count);

    Collection<AlbumContext> getAlbumContexts();

    Collection<Album> getAlbums();

    @DAC
    int getAlbumCount();

    @DAC
    List<AlbumContext> getAlbumsByYear();

    default Collection<Track> getTracks() {
        return getTracks(false);
    }

    Stream<Track> getTracksFeaturing(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Stream<Album> getAlbum(UUID id);

    Collection<Series> getSeries();

    @DAC
    List<AlbumContext> getAlbumsFeaturing(Artist id);

    Stream<Artist> getArtist(UUID id);

    Stream<Series> getSeries(UUID id);

    Stream<Artist> getArtist(String name);

    Stream<Album> getAlbum(String albumName);

    boolean isEmpty();

    Stream<AlbumContext> getAlbumContext(UUID uuid);

    private static Media combineMedia(Media baseMedia, DiscogsDataResolver discogsData) {
        log.info("Adding {} to {}", discogsData, baseMedia);
        Stream<Album> stream = baseMedia.getAlbums().stream();
        Collection<AlbumContext> albumContexts = stream
            .map(album ->
                discogsData.getDiscogRelease(album)
                    .map(digest ->
                        buildAlbumContext(album, digest))
                    .orElseGet(() ->
                        new AlbumContext(album)))
            .collect(Collectors.toList());
        return baseMedia.withAlbumContexts(albumContexts);
    }

    private static DiscogsDataResolver scanDiscogsData(
        Path resourcesPath,
        Media baseMedia,
        iTunesLibrary iTunesLibrary
    ) {
        log.info("Reading discogs data");
        Collection<DiscogConnection> metaConnections = metaConnections(baseMedia, iTunesLibrary);
        DiscogsDataResolver discogsData = new DiscogsDataResolver(
            resourcesPath,
            metaConnections,
            FORTNITE,
            Clock.systemDefaultZone());
        log.info("Retrieved {} discogs data", discogsData.getConnections().size());
        return discogsData;
    }

    private static iTunesLibrary scanLibrary(Path libraryPath) {
        log.info("Reading iTunes library from {}", libraryPath);
        iTunesLibrary iTunesLibrary = iTunesLibrary(libraryPath);
        log.info("Read {} entries", iTunesLibrary.getTracks().size());
        return iTunesLibrary;
    }

    private static Media scanMedia(Path mediaPath) {
        log.info("Scanning from {}", mediaPath);
        Media baseMedia = new LocalMedia(mediaPath);
        log.info("Scanned {} albums: {}", baseMedia.getAlbums().size(), baseMedia);
        return baseMedia;
    }

    private static AlbumContext buildAlbumContext(Album album, DiscogReleaseDigest digest) {
        AlbumContext context = Stream.of(digest.getArtists(), digest.getExtraartists())
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .reduce(
                new AlbumContext(
                    album,
                    yearOf(digest),
                    id(digest.getUri()),
                    URI.create(digest.getUri()),
                    cover150(digest).orElse(null),
                    cover(digest).orElse(null),
                    digest.getNotes(),
                    series(digest),
                    videos(digest)),
                (albumContext, artistDigest) ->
                    albumContext.credit(
                        artistDigest.getName(),
                        artistDigest.getUri(),
                        artistDigest.getRole()),
                noCombine());
        Collection<TrackContext> trackContexts = digest.getTracklist().stream()
            .map(track ->
                new TrackContext(track.getPosition(), track.getTitle(), trackCredits(track)))
            .collect(Collectors.toList());
        List<TrackContext> applicableTrackContexts = trackContexts.stream().map(trackContext ->
            trackContext.getTrackNo().flatMap(trackNo ->
                trackContext.getDisc()
                    .map(disc ->
                        album.getTrack(disc, trackNo))
                    .orElseGet(() ->
                        album.getTrack(trackNo)))
                .map(trackContext::withTrack)
                .orElse(trackContext)).collect(Collectors.toList());
        return context.withTrackContexts(applicableTrackContexts);
    }

    private static boolean isPrimary(DiscogImage image) {
        return "primary".equalsIgnoreCase(image.getType());
    }

    private static boolean hasImage(DiscogImage image) {
        return image.getUri150() != null;
    }
}

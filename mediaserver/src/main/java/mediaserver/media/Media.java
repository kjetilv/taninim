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
import java.util.function.BiFunction;
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

    Media sortedAlbums(Comparator<Album> comparator);

    Media withAlbumContext(UUID albumId, AlbumContext albumContext);

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

    Collection<Album> getRandomAlbums(int count);

    Collection<Album> getAlbums();

    @DAC
    Collection<Album> getAlbumsByYear();

    default Collection<Track> getTracks() {
        return getTracks(false);
    }

    Stream<Track> getTracksFeaturing(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Stream<Album> getAlbum(UUID id);

    Collection<Series> getSeries();

    @DAC
    Collection<Album> getAlbumsFeaturing(Artist id);

    Stream<Artist> getArtist(UUID id);

    Stream<Series> getSeries(UUID id);

    Stream<Artist> getArtist(String name);

    Stream<Album> getAlbum(String albumName);

    static Media local(Path mediaPath, Path libraryPath, Path resourcesPath) {
        log.info("Scanning from {}", mediaPath);
        Media baseMedia = new LocalMedia(mediaPath);
        log.info("Scanned {} albums: {}", baseMedia.getAlbums().size(), baseMedia);
        log.info("Reading iTunes library from {}", libraryPath);
        iTunesLibrary iTunesLibrary = iTunesLibrary(libraryPath);
        log.info("Read {} entries", iTunesLibrary.getTracks().size());
        log.info("Reading discogs data");
        Collection<DiscogConnection> metaConnections = metaConnections(baseMedia, iTunesLibrary);
        DiscogsDataResolver discogsData = new DiscogsDataResolver(
            resourcesPath,
            metaConnections,
            FORTNITE,
            Clock.systemDefaultZone());
        log.info("Retrieved {} discogs data", discogsData.getConnections().size());
        Media media = baseMedia.getAlbums().stream()
            .reduce(baseMedia, addContextFrom(discogsData), noCombine());
        log.info("Returning {}", media);
        return media;
    }

    static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("NO combine");
        };
    }

    static BiFunction<Media, Album, Media> addContextFrom(DiscogsDataResolver discogsData) {
        return (media, album) ->
            discogsData.getDiscogRelease(album).map(digest -> {
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
                        (ctx, dad) ->
                            ctx.credit(
                                dad.getName(),
                                dad.getUri(),
                                dad.getRole()),
                        noCombine());
                List<TrackContext> trackContexts = digest.getTracklist().stream()
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
                AlbumContext albumContext =
                    context.withTrackContexts(applicableTrackContexts);
                return media.withAlbumContext(album.getUuid(), albumContext);
            }).orElse(media);
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

    static List<String> series(DiscogReleaseDigest release) {
        return release.getSeries().stream().map(DiscogSeriesDigest::getName).collect(Collectors.toList());
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

    boolean isEmpty();

    private static boolean isPrimary(DiscogImage image) {
        return "primary".equalsIgnoreCase(image.getType());
    }

    private static boolean hasImage(DiscogImage image) {
        return image.getUri150() != null;
    }

    enum AlbumSort {
        ARTIST, TITLE, YEAR
    }
}

package mediaserver.media;

import mediaserver.externals.*;
import mediaserver.util.IO;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

    int SE7EN = 7;

    default Media subLibrary(Series series) {

        return subLibrary(null, null, series, null);
    }

    default Media subLibrary(Artist artist) {

        return subLibrary(null, artist, null, null);
    }

    default Media subLibrary(Playlist playlist) {

        return subLibrary(null, null, null, playlist);
    }

    Media subLibrary(CategoryPath categoryPath, Artist artist, Series series, Playlist playlist);

    Media withAlbumContext(UUID albumId, AlbumContext albumContext);

    Optional<Track> getTrack(UUID uuid);

    default boolean isSubCategories() {

        return !getTopCategories().isEmpty();
    }

    Year getStartYear();

    Year getEndYear();

    CategoryPath getCategoryPath();

    Optional<CategoryPath> getCategoryPath(UUID uuid);

    Collection<CategoryPath> getTopCategories();

    Collection<CategoryPath> getCategories();

    Collection<Playlist> getPlaylists();

    Optional<Playlist> getPlaylist(UUID uuid);

    Collection<Playlist> getCurations();

    Optional<Playlist> getCuration(UUID uuid);

    boolean isCurated(Track track);

    Duration getDuration();

    default String getPrettyDuration() {

        return Print.pretty(getDuration());
    }

    default Collection<Artist> getAlbumArtists() {

        return getAlbumArtists(false);
    }

    default Collection<Artist> getAllAlbumArtists() {

        return getAlbumArtists(true);
    }

    Collection<Artist> getAlbumArtists(boolean recurse);

    default Collection<Artist> getArtists() {

        return getArtists(false);
    }

    Collection<Artist> getArtists(boolean recurse);

    Collection<Artist> getAlbumCreditedArtists();

    Collection<Artist> getTrackCreditedArtists();

    default Collection<Album> getSevenRandomAlbums() {

        return getRandomAlbums(SE7EN);
    }

    Collection<Album> getRandomAlbums(int count);

    default Collection<Album> allAlbums() {

        return getAlbums(true);
    }

    default Collection<Album> getAlbums() {

        return getAlbums(false);
    }

    Collection<Album> getAlbums(boolean recurse);

    default Collection<Track> getTracks() {

        return getTracks(false);
    }

    default Collection<Track> getAllTracks() {

        return getTracks(true);
    }

    Collection<Track> getTracksFeaturing(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Optional<Album> getAlbum(UUID id);

    Collection<Series> getSeries();

    Collection<Album> getAlbumsFeaturing(Artist id);

    Optional<Artist> getArtist(UUID id);

    Optional<Series> getSeries(UUID id);

    Optional<Artist> getArtist(String name);

    Optional<Album> getAlbum(String albumName);

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
            Duration.ofDays(14),
            Clock.systemDefaultZone());
        log.info("Retrieved {} discogs data", discogsData.getConnections().size());

        Media media = baseMedia.allAlbums().stream()
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
            discogsData.getDiscogRelease(album).map(release -> {

                AlbumContext context = Stream.of(release.getArtists(), release.getExtraartists())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .reduce(
                        new AlbumContext(
                            album,
                            yearOf(release),
                            URI.create(release.getUri()),
                            cover(release).orElse(null),
                            release.getNotes(),
                            series(release),
                            videos(release)),
                        (ctx, dad) ->
                            ctx.credit(
                                dad.getName(),
                                dad.getUri(),
                                dad.getRole()),
                        noCombine());
                List<TrackContext> trackContexts = release.getTracklist().stream().map(
                    track -> {
                        Credits trackCredits = Optional.ofNullable(track.getExtraartists()).stream().flatMap(
                            Collection::stream).reduce(
                            new Credits(),
                            (credits, dad) ->
                                credits.credit(dad.getName(), dad.getUri(), dad.getRole()),
                            noCombine());
                        return new TrackContext(
                            track.getPosition(),
                            track.getTitle(),
                            trackCredits);
                    })
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
                return media.withAlbumContext(
                    album.getUuid(), context.withTrackContexts(trackContexts));
            }).orElse(media);
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

    static Optional<URI> cover(DiscogReleaseDigest release) {

        return Stream.concat(
            release.getImages().stream().filter(Media::isPrimary),
            release.getImages().stream().filter(Media::hasImage)
        ).filter(Media::hasImage)
            .findFirst()
            .map(DiscogImage::getUri150);
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
            .map(track ->
                media.getAlbum(track.getAlbum())
                    .map(album ->
                        new DiscogConnection(
                            album,
                            URI.create(track.getComments().trim()))))
            .filter(Optional::isPresent).map(Optional::get)
            .distinct()
            .collect(Collectors.toList());
    }

    static iTunesLibrary iTunesLibrary(Path libraryPath) {

        try {
            Map<String, ?> plist = IO.readFromStream(libraryPath, new IOSMapParser()::convert);
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
}

package mediaserver.media;

import mediaserver.externals.*;
import mediaserver.util.DAC;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

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

    default Collection<Track> getTracks() {

        return getTracks(false);
    }

    Stream<Track> getTracksFeaturing(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Stream<Album> getAlbum(UUID id);

    Collection<Series> getSeries();

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
            Duration.ofDays(14),
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
            discogsData.getDiscogRelease(album).map(release -> {
                AlbumContext context = Stream.of(release.getArtists(), release.getExtraartists())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .reduce(
                        new AlbumContext(
                            album,
                            yearOf(release),
                            URI.create(release.getUri()),
                            cover150(release).orElse(null),
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
                List<TrackContext> trackContexts = release.getTracklist().stream()
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
                return media.withAlbumContext(
                    album.getUuid(), context.withTrackContexts(applicableTrackContexts));
            }).orElse(media);
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

    static Optional<URI> getCover(DiscogReleaseDigest release, Function<DiscogImage, URI> getUri150) {

        return Stream.concat(
            release.getImages().stream().filter(Media::isPrimary),
            release.getImages().stream().filter(Media::hasImage)
        ).filter(Media::hasImage)
            .findFirst()
            .map(getUri150);
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
}

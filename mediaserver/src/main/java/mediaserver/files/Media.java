package mediaserver.files;

import mediaserver.externals.DiscogReleaseDigest;
import mediaserver.externals.DiscogSeriesDigest;
import mediaserver.externals.XmlMapParser;
import mediaserver.externals.iTunesLibrary;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.Year;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

    Media subLibrary(CategoryPath categoryPath, Artist artist);

    Media withAlbumContext(UUID albumId, AlbumContext albumContext);

    Optional<Track> getTrack(UUID uuid);

    default boolean isSubCategories() {

        return !getTopCategories().isEmpty();
    }

    CategoryPath getCategoryPath();

    Optional<CategoryPath> getCategoryPath(UUID uuid);

    Collection<CategoryPath> getTopCategories();

    Collection<CategoryPath> getCategories();

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

    Collection<Artist> getAllArtists();

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

    Collection<Track> getTracksBy(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Optional<Album> getAlbum(UUID id);

    Collection<Album> getAlbumsFeaturing(Artist id);

    Optional<Artist> getArtist(UUID id);

    Optional<Artist> getArtist(String name);

    Optional<Album> getAlbum(String albumName);

    static Media local(String file, String library, String resources) {

        return local(
            new File(file).toPath(),
            new File(library).toPath(),
            new File(resources).toPath());
    }

    static Media local(Path mediaPath, Path libraryPath, Path resourcesPath) {

        log.info("Scanning from {}", mediaPath);
        Media baseMedia = new LocalMedia(mediaPath);
        log.info("Scanned: {}", baseMedia);

        log.info("Reading from {}", libraryPath);

        iTunesLibrary iTunesLibrary = iTunesLibrary(libraryPath);
        Collection<DiscogConnection> metaConnections = metaConnections(baseMedia, iTunesLibrary);
        DiscogsDataResolver discogsData = new DiscogsDataResolver(resourcesPath, metaConnections);
        Media media = baseMedia.allAlbums().stream().reduce(baseMedia, addContextFrom(discogsData), noCombine());
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
                AlbumContext context = Optional.ofNullable(release.getExtraartists()).stream()
                    .flatMap(Collection::stream)
                    .reduce(
                        new AlbumContext(
                            album,
                            yearOf(release),
                            URI.create(release.getUri()),
                            release.getNotes(),
                            series(release)),
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
                            trackCredits
                        );
                    })
                    .collect(Collectors.toList());
                return media.withAlbumContext(album.getUuid(), context.withTrackContexts(trackContexts));
            }).orElse(media);
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
                            URI.create(track.getComments()))))
            .filter(Optional::isPresent).map(Optional::get)
            .distinct()
            .collect(Collectors.toList());
    }

    static iTunesLibrary iTunesLibrary(Path libraryPath) {

        try {
            Map<String, ?> plist = IO.readStream(libraryPath, new XmlMapParser()::convert);
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
}

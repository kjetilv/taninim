package mediaserver.gui;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import mediaserver.Config;
import mediaserver.GlobalState;
import mediaserver.hash.Hashable;
import mediaserver.hash.Namable;
import mediaserver.http.Handling;
import mediaserver.http.Link;
import mediaserver.http.QPar;
import mediaserver.http.QueryParametersTracker;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.media.Album;
import mediaserver.media.Artist;
import mediaserver.media.Media;
import mediaserver.media.Playlist;
import mediaserver.media.Series;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;
import mediaserver.util.Print;
import mediaserver.util.Ran;

public final class IndexPage extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    private final Supplier<Media> media;

    public IndexPage(Route route, Supplier<Media> media, Templater templater) {
        super(route, templater);
        this.media = media;
    }

    protected @Override @Nonnull Handling handle(Req req) {
        Template template = template(req, media.get());
        return respondHtml(req, template);
    }

    private Template template(Req req, Media media) {
        Collection<Artist> currentArtists = QPar.artist.id(req).flatMap(media::getArtist).collect(Collectors.toList());
        Collection<Series> currentSeries = QPar.series.id(req).flatMap(media::getSeries).collect(Collectors.toList());
        Collection<Playlist> currentCurations =
            QPar.curation.id(req).flatMap(media::getCuration).collect(Collectors.toList());
        Collection<Playlist> currentPlaylists =
            QPar.playlist.id(req).flatMap(media::getPlaylist).collect(Collectors.toList());
        QueryParametersTracker tracker = new QueryParametersTracker().set(QPar.artist, currentArtists)
            .set(QPar.series, currentSeries)
            .set(QPar.playlist, currentPlaylists)
            .set(QPar.curation, currentCurations);
        boolean union = QPar.union.isTrue(req);
        Media submedia = media
            .subLibrary(currentArtists, currentSeries, currentPlaylists, currentCurations, union)
            .sortedAlbums(albumComparator(QPar.sort.params(req)));
        Collection<Link<Artist>> artistLinks = links(currentArtists, linker(QPar.artist, tracker));
        Collection<Link<Series>> seriesLinks = links(currentSeries, linker(QPar.series, tracker));
        Collection<Link<Playlist>> playlistLinks = links(currentPlaylists, linker(QPar.playlist, tracker));
        Collection<Link<Playlist>> curationLinks = links(currentCurations, linker(QPar.curation, tracker));
        Collection<Link<Artist>> albumArtistsLinks = links(submedia.getAllAlbumArtists(), linker(QPar.artist, tracker));
        Collection<Link<Series>> mediaSeriesLinks = links(submedia.getSeries(), linker(QPar.series, tracker));
        Collection<Link<Playlist>> mediaPlaylistLinks = links(submedia.getPlaylists(), linker(QPar.playlist, tracker));
        Collection<Link<Playlist>> mediaCurationLinks = links(submedia.getCurations(), linker(QPar.curation, tracker));
        boolean streamHighlighted = req.getSession().getAccessLevel().satisfies(AccessLevel.STREAM_SINGLE);
        Instant time = req.getTime();
        Optional<Pair<Album, Track>> globalTrack =
            streamHighlighted ? GlobalState.get().getGlobalTrack(time) : Optional.empty();
        Optional<Pair<Album, Track>> highlightedAlbumAndTrack =
            streamHighlighted ? globalTrack.or(() -> req.getSession()
                .getSessionState()
                .getRandomTrack(time, () -> randomAlbumTrack(submedia))) : Optional.empty();
        Optional<Duration> highlightedTimeRemaining =
            highlightedAlbumAndTrack.isEmpty()
                ? Optional.empty()
                : globalTrack.isPresent()
                    ? GlobalState.get().getGlobalTrackRemaining(time)
                    : req.getSession().getSessionState().getRandomTrackRemaining(time);
        Optional<Album> highlightedAlbum = highlightedAlbumAndTrack.map(Pair::getT1);
        Optional<Track> highlightedTrack = highlightedAlbumAndTrack.map(Pair::getT2);
        Template
            template =
            getTemplate(INDEX_PAGE).add(TPar.plyr, Config.PLYR)
                .add(TPar.highlightedAlbum, highlightedAlbum)
                .add(TPar.highlightedArtist, highlightedAlbum.map(Album::getArtist).map(linker(QPar.artist, tracker)))
                .add(TPar.highlighted, highlightedTrack)
                .add(TPar.highlightedSelected, globalTrack.isPresent())
                .add(TPar.highlightedRemaining, highlightedTimeRemaining.map(Print::prettyLongTime))
                .add(TPar.randomAlbums, submedia.getRandomAlbums(7))
                .add(TPar.user, req.getSession().getActiveUser(req))
                .add(TPar.media, submedia)
                .add(TPar.artist, artistLinks.stream().findFirst().orElse(null))
                .add(TPar.artists, artistLinks)
                .add(TPar.albumArtists, albumArtistsLinks)
                .add(TPar.series, seriesLinks)
                .add(TPar.playlists, playlistLinks)
                .add(TPar.curations, curationLinks)
                .add(TPar.mediaSeries, mediaSeriesLinks)
                .add(TPar.mediaPlaylists, mediaPlaylistLinks)
                .add(TPar.mediaCurations, mediaCurationLinks);
        if (tracker.isMulti()) {
            return template.add(TPar.union, union).add(TPar.unionLink, tracker.set(QPar.union, !union));
        }
        return template;
    }

    private static Comparator<Album> albumComparator(Stream<String> params) {
        Media.AlbumSort sort =
            params.map(Media.AlbumSort::valueOf).findFirst().orElse(Media.AlbumSort.TITLE);
        switch (sort) {
            case ARTIST -> {
                return Comparator.comparing(Album::getArtist);
            }
            case YEAR -> {
                return Comparator.comparing((Album a) -> a.getContext().getYear());
            }
            case TITLE -> {
                return Comparator.comparing(Album::getName);
            }
            default -> throw new IllegalStateException("No such sort: " + sort);
        }
    }

    private static <T extends Hashable & Namable> Collection<Link<T>> links(
        Collection<T> currentArtists, Function<T, Link<T>> linker
    ) {
        return currentArtists.stream().map(linker).collect(Collectors.toList());
    }

    private static <T extends Hashable & Namable> Function<T, Link<T>> linker(
        QPar qpar, QueryParametersTracker tracker
    ) {
        return t -> new Link<>(t, tracker.add(qpar, t), tracker.remove(qpar, t), tracker.focus(qpar, t));
    }

    private static Optional<Pair<Album, Track>> randomAlbumTrack(Media submedia) {
        return Ran.dom(submedia.getRandomAlbums(20))
            .flatMap(album -> Ran.dom(album.getTracks()).map(track -> Pair.of(album, track)));
    }
}

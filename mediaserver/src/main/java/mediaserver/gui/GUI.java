package mediaserver.gui;

import mediaserver.Config;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.Session;
import mediaserver.stream.Streamer;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mediaserver.sessions.AccessLevel.STREAM;
import static mediaserver.sessions.AccessLevel.STREAM_CURATED;

public final class GUI extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    private static final Logger log = LoggerFactory.getLogger(GUI.class);

    private final Supplier<Media> media;

    public GUI(Supplier<Media> media, Templater templater) {

        super(templater, Page.INDEX, Page.ALBUM);
        this.media = media;
    }

    @Override
    protected Handling handleRequest(Req req) {

        return template(req, media.get())
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Optional<Template> template(Req req, Media media) {

        QPars pars = req.getQueryParameters();
        if (req.isFor(Page.ALBUM)) {
            return albumTemplate(media, req, pars);
        }
        return indexTemplate(req, media, pars);
    }

    private Optional<Template> indexTemplate(Req req, Media media, QPars pars) {

        Artist artist =
            pars.get(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.get(QPar.SERIES).flatMap(media::getSeries).orElse(null);
        Playlist curation =
            pars.get(QPar.CURATION).flatMap(media::getCuration).orElse(null);
        Playlist playlist = curation == null
            ? pars.get(QPar.PLAYLIST).flatMap(media::getPlaylist).orElse(null)
            : null;

        return Optional.ofNullable(
            media.subLibrary(null, artist, series, curation == null ? playlist : curation))
            .filter(submedia -> !submedia.isEmpty())
            .map(submedia ->
                base(getTemplate(INDEX_PAGE), req, submedia)
                    .add(TPar.ARTIST, artist)
                    .add(TPar.SERIES, series)
                    .add(TPar.PLAYLIST, playlist)
                    .add(TPar.CURATION, curation));
    }

    private Optional<Template> albumTemplate(Media media, Req req, QPars pars) {

        return pars.get(QPar.ALBUM)
            .flatMap(media::getAlbum)
            .map(album ->
                albumTemplate(media, req, pars, album))
            .or(() -> {
                log.warn("Was asked for {}", pars.get(QPar.ALBUM)
                    .map(unknown -> "unknown album " + unknown)
                    .orElse("unspecified album"));
                return indexTemplate(req, media, pars);
            });
    }

    private Template albumTemplate(Media media, Req req, QPars pars, Album album) {

        Optional<Selected> selectedTrack = track(media, req, pars, QPar.TRACK)
            .flatMap(track ->
                selected(media, req, pars, album, track));
        return base(getTemplate(ALBUM_PAGE), req, media)
            .add(TPar.ALBUM, album)
            .add(TPar.SELECTED, selectedTrack)
            .add(TPar.PLAYABLE_GROUPS, playableGroups(media, req, album))
            .add(TPar.PLAYLISTS, Playlist.playlistsWith(album))
            .add(TPar.CURATIONS, Playlist.curationsWith(album));
    }

    private Optional<Selected> selected(Media media, Req req, QPars pars, Album album, Track track) {

        return Optional.ofNullable(req.getSession())
            .filter(session ->
                streamable(media, track, session))
            .map(session ->
                new Selected(
                    track,
                    autoplay(media, req, pars, track),
                    previousTrack(track, album.getTracks()).orElse(null),
                    nextTrack(track, album.getTracks()).orElse(null)));
    }

    private boolean streamable(Media media, Track track, Session session) {

        return session.hasLevel(STREAM) || session.hasLevel(STREAM_CURATED) && media.isCurated(track);
    }

    private boolean autoplay(Media media, Req req, QPars pars, Track track) {

        return track(media, req, pars, QPar.AUTOPLAY).filter(track::equals).isPresent();
    }

    private Collection<PlayableGroup> playableGroups(Media media, Req req, Album album) {

        return album.getTracksByPart().entrySet().stream()
            .map(e ->
                new PlayableGroup(
                    e.getKey(),
                    e.getValue().stream()
                        .map(t ->
                            new Playable(t, streamable(media, t, req.getSession())))
                        .collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    private Optional<Track> track(Media media, Req req, QPars pars, QPar track1) {

        return pars.get(track1)
            .flatMap(media::getTrack)
            .filter(Streamer.authorized(media, req));
    }

    private Optional<Track> previousTrack(Track track, Collection<Track> tracks) {

        if (track.getTrackNo() > 1) {
            return tracks.stream()
                .filter(t -> Objects.equals(t.getPart(), track.getPart()))
                .filter(t -> t.getTrackNo() == track.getTrackNo() - 1)
                .findFirst();
        }

        if (track.getPart() == null) {
            return Optional.empty();
        }

        return previousPart(track, tracks)
            .flatMap(previousPart ->
                tracks.stream()
                    .filter(t ->
                        previousPart.equals(t.getPart()))
                    .max(trackNo()));
    }

    private Optional<Track> nextTrack(Track track, Collection<Track> tracks) {

        LinkedList<Track> tracksInPart = tracks.stream()
            .filter(samePart(track))
            .sorted(trackNo())
            .collect(Collectors.toCollection(LinkedList::new));

        if (track.equals(tracksInPart.getLast())) {
            return nextPart(track, tracks)
                .flatMap(nextPart ->
                    tracks.stream()
                        .filter(t -> nextPart.equals(t.getPart()))
                        .min(trackNo()));
        }

        return tracksInPart.stream()
            .filter(t -> t.getTrackNo() > track.getTrackNo())
            .min(trackNo());
    }

    private Predicate<Track> samePart(Track track) {

        return t ->
            Objects.equals(track.getPart(), t.getPart());
    }

    private Stream<Integer> parts(Collection<Track> tracks) {

        return tracks.stream().map(Track::getPart).filter(Objects::nonNull);
    }

    private Optional<Integer> nextPart(Track track, Collection<Track> tracks) {

        return parts(tracks).filter(part -> part > track.getPart()).min(Integer::compareTo);
    }

    private Optional<Integer> previousPart(Track track, Collection<Track> tracks) {

        return parts(tracks).filter(part -> part < track.getPart()).max(Integer::compareTo);
    }

    private Comparator<Track> trackNo() {

        return Comparator.comparing(Track::getTrackNo);
    }

    private Template base(Template template, Req req, Media media) {

        return template
            .add(TPar.USER, req.getSession().getActiveUser(req))
            .add(TPar.MEDIA, media)
            .add(TPar.PLYR, Config.PLYR);
    }
}

package mediaserver.gui;

import mediaserver.Config;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.AccessLevel;
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
            .map(album -> {

                Collection<Track> tracks = playTracks(media, req, album);

                Optional<Track> track = track(media, req, pars, QPar.TRACK);
                Optional<Track> autoplay = track(media, req, pars, QPar.AUTOPLAY);
                Optional<Track> previousTrack = track.flatMap(t -> previousTrack(t, tracks));
                Optional<Track> nextTrack = track.flatMap(t -> nextTrack(t, tracks));


                Template template = base(getTemplate(ALBUM_PAGE), req, media)
                    .add(TPar.ALBUM, album)
                    .add(TPar.PLAYLISTS, Playlist.playlistsWith(album))
                    .add(TPar.PREVIOUS_TRACK, previousTrack.orElse(null))
                    .add(TPar.PLAY_TRACK, track.orElse(null))
                    .add(TPar.AUTOPLAY, track.equals(autoplay))
                    .add(TPar.NEXT_TRACK, nextTrack.orElse(null))
                    .add(TPar.PLAY_TRACKS, tracks);

                return isCuratedAccessOnly(req.getSession())
                    ? template.add(TPar.CURATIONS, Playlist.curationsWith(album))
                    : template;
            })
            .or(() -> {

                log.warn("Was asked for {}", pars.get(QPar.ALBUM)
                    .map(unknown -> "unknown album " + unknown)
                    .orElse("unspecified album"));
                return indexTemplate(req, media, pars);
            });
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

    private Collection<Track> playTracks(Media media, Req req, Album album) {

        return album.getTracks().stream()
            .filter(Streamer.authorized(media, req))
            .collect(Collectors.toList());
    }

    private boolean isCuratedAccessOnly(Session session) {

        return session.hasLevel(AccessLevel.STREAM_CURATED) && !session.hasLevel(AccessLevel.STREAM);
    }

    private Template base(Template template, Req req, Media media) {

        return template
            .add(TPar.USER, req.getSession().getActiveUser(req))
            .add(TPar.MEDIA, media)
            .add(TPar.PLYR, Config.PLYR);
    }
}

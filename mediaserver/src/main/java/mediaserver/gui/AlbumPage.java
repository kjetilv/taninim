package mediaserver.gui;

import mediaserver.Config;
import mediaserver.Globals;
import mediaserver.http.*;
import mediaserver.media.Album;
import mediaserver.media.Media;
import mediaserver.media.Playlist;
import mediaserver.media.Track;
import mediaserver.sessions.Session;
import mediaserver.stream.Streamer;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mediaserver.sessions.AccessLevel.*;

public final class AlbumPage extends TemplateEnabled {

    private final Supplier<Media> media;

    public AlbumPage(Supplier<Media> media, Templater templater) {

        super(templater, Route.ALBUM);
        this.media = media;
    }

    @Override
    protected Handling handle(Req req) {

        return template(req, media.get())
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Optional<Template> template(Req req, Media media) {

        QPars pars = req.getQueryParameters();

        return pars.get(QPar.album)
            .flatMap(media::getAlbum)
            .map(album ->
                albumTemplate(media, req, pars, album));
    }

    private Template albumTemplate(Media media, Req req, QPars pars, Album album) {

        Optional<Selected> selectedTrack = selectedTrack(media, req, pars, album);
        return base(getTemplate(ALBUM_PAGE), req, media)
            .add(TPar.admin, req.getSession().getAccessLevel().satisfies(ADMIN))
            .add(TPar.album, album)
            .add(TPar.selected, selectedTrack)
            .add(TPar.trackHighlighted, selectedTrack.filter(isHighlighted(req)).isPresent())
            .add(TPar.playableGroups, playableGroups(media, req, album))
            .add(TPar.playlists, Playlist.playlistsWith(album))
            .add(TPar.curations, Playlist.curationsWith(album));
    }

    private Optional<Selected> selectedTrack(Media media, Req req, QPars pars, Album album) {

        return track(media, req, pars, QPar.track)
            .flatMap(track ->
                selected(media, req, pars, album, track));
    }

    private static Predicate<Selected> isHighlighted(Req req) {

        return selectedTrack ->
            Optional.ofNullable(selectedTrack.getTrack())
                .filter(st ->
                    Globals.get().getGlobalTrack(req.getTime()).map(Pair::getT2).filter(st::equals).isPresent())
                .isPresent();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static boolean isHighlighted(
        Optional<Pair<Album, Track>> accessibleTrack,
        Optional<Selected> selectedTrack
    ) {

        return selectedTrack
            .map(Selected::getTrack)
            .map(st ->
                accessibleTrack.map(Pair::getT2).filter(st::equals))
            .isPresent();
    }

    private static Optional<Selected> selected(Media media, Req req, QPars pars, Album album, Track track) {

        return Optional.ofNullable(req.getSession())
            .filter(session ->
                streamable(media, track, session))
            .map(session ->
                new Selected(
                    album,
                    track,
                    autoplay(media, req, pars, track),
                    previousTrack(track, album.getTracks()).orElse(null),
                    nextTrack(track, album.getTracks()).orElse(null)));
    }

    private static boolean streamable(Media media, Track track, Session session) {

        return session.hasLevel(STREAM) || session.hasLevel(STREAM_CURATED) && media.isCurated(track);
    }

    private static boolean autoplay(Media media, Req req, QPars pars, Track track) {

        return track(media, req, pars, QPar.autoplay).filter(track::equals).isPresent();
    }

    private static Collection<PlayableGroup> playableGroups(Media media, Req req, Album album) {

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

    private static Optional<Track> track(Media media, Req req, QPars pars, QPar track1) {

        return pars.get(track1)
            .flatMap(media::getTrack)
            .filter(Streamer.authorized(media, req));
    }

    private static Optional<Track> previousTrack(Track track, Collection<Track> tracks) {

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

    private static Optional<Track> nextTrack(Track track, Collection<Track> tracks) {

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

    private static Predicate<Track> samePart(Track track) {

        return t ->
            Objects.equals(track.getPart(), t.getPart());
    }

    private static Stream<Integer> parts(Collection<Track> tracks) {

        return tracks.stream().map(Track::getPart).filter(Objects::nonNull);
    }

    private static Optional<Integer> nextPart(Track track, Collection<Track> tracks) {

        return parts(tracks).filter(part -> part > track.getPart()).min(Integer::compareTo);
    }

    private static Optional<Integer> previousPart(Track track, Collection<Track> tracks) {

        return parts(tracks).filter(part -> part < track.getPart()).max(Integer::compareTo);
    }

    private static Comparator<Track> trackNo() {

        return Comparator.comparing(Track::getTrackNo);
    }

    private static Template base(Template template, Req req, Media media) {

        return template
            .add(TPar.user, req.getSession().getActiveUser(req))
            .add(TPar.media, media)
            .add(TPar.plyr, Config.PLYR);
    }
}

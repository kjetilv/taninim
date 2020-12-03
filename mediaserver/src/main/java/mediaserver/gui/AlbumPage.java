package mediaserver.gui;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.Config;
import mediaserver.GlobalState;
import mediaserver.http.Handling;
import mediaserver.http.Par;
import mediaserver.http.QPar;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.media.AlbumContext;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Media;
import mediaserver.media.Playlist;
import mediaserver.media.Track;
import mediaserver.sessions.Session;
import mediaserver.stream.StreamAuthorization;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;

import static mediaserver.sessions.AccessLevel.ADMIN;
import static mediaserver.sessions.AccessLevel.STREAM;
import static mediaserver.sessions.AccessLevel.STREAM_CURATED;

public final class AlbumPage extends TemplateEnabled {

    private final Supplier<? extends Media> media;

    public AlbumPage(Route route, Supplier<? extends Media> media, Templater templater) {
        super(route, templater);
        this.media = Objects.requireNonNull(media, "media");
    }

    @Override
    protected Handling handle(Req req) {
        return template(req, media.get()).findFirst()
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Stream<Template> template(Req req, Media media) {
        return QPar.album.id(req)
            .flatMap(media::getAlbumContext)
            .map(album ->
                albumTemplate(media, req, album));
    }

    private Template albumTemplate(Media media, Req req, AlbumContext albumContext) {
        Optional<SelectedTrack> selectedTrack =
            selectedTrack(media, req, albumContext).findFirst();
        return getTemplate(ALBUM_PAGE)
            .add(TPar.user, req.getSession().getActiveUser(req))
            .add(TPar.media, media)
            .add(TPar.plyr, Config.PLYR)
            .add(TPar.admin, req.getSession().getAccessLevel().satisfies(ADMIN))
            .add(TPar.albumContext, albumContext)
            .add(TPar.compressed, req.isLocal() && req.isFlac())
            .add(TPar.selected, selectedTrack)
            .add(TPar.trackHighlighted, selectedTrack.filter(isHighlighted(req)))
            .add(TPar.playableGroups, playableGroups(media, req, albumContext))
            .add(TPar.albumPlayable, albumPlayable(media, req, albumContext))
            .add(TPar.playlists, Playlist.playlistsWith(albumContext))
            .add(TPar.curations, Playlist.curationsWith(albumContext))
            .add(TPar.format, req.isLocal() ? FLAC : M4A);
    }

    private static final String M4A = "m4a";

    private static final String FLAC = "flac";

    private static Predicate<SelectedTrack> isHighlighted(Req req) {
        return selectedTrack ->
            GlobalState.get().getGlobalTrack(req.getTime())
                .map(Pair::getT2)
                .filter(selectedTrack.getTrack()::equals)
                .isPresent();
    }

    private static Stream<SelectedTrack> selectedTrack(Media media, Req req, AlbumContext albumContext) {
        return track(media, req, albumContext, QPar.track).flatMap(track ->
            Stream.of(req.getSession())
                .filter(session ->
                    streamable(media, track, session))
                .map(session ->
                    new SelectedTrack(
                        track,
                        autoplay(media, req, albumContext, track),
                        previousTrack(track.getTrack(), track.getAlbum().getTracks()).orElse(null),
                        nextTrack(track.getTrack(), track.getAlbum().getTracks()).orElse(null))));
    }

    private static boolean streamable(Media media, AlbumTrack albumTrack, Session session) {
        return session.hasLevel(STREAM) || session.hasLevel(STREAM_CURATED) && media.isCurated(albumTrack);
    }

    private static boolean autoplay(
        Media media,
        Req req,
        AlbumContext albumContext,
        AlbumTrack albumTrack
    ) {
        return track(media, req, albumContext, QPar.autoplay)
            .anyMatch(albumTrack::equals);
    }

    private static Collection<PlayableGroup> playableGroups(Media media, Req req, AlbumContext albumContext) {
        return playables(media, req, albumContext).collect(Collectors.toList());
    }

    private static Optional<Playable> albumPlayable(Media media, Req req, AlbumContext albumContext) {
        return playables(media, req, albumContext)
            .map(PlayableGroup::getPlayables)
            .flatMap(Collection::stream)
            .findFirst();
    }

    private static Stream<PlayableGroup> playables(Media media, Req req, AlbumContext albumContext) {
        return albumContext.getAlbum().getTracksByPart().entrySet().stream()
            .map(e -> new PlayableGroup(
                e.getKey(),
                e.getValue()
                    .stream()
                    .map(track -> new Playable(
                        track,
                        streamable(
                            media,
                            new AlbumTrack(albumContext, track),
                            req.getSession())))
                    .collect(Collectors.toList())));
    }

    private static Stream<AlbumTrack> track(
        Media media,
        Req req,
        AlbumContext albumContext,
        Par<? super Req, String> idParam
    ) {
        Stream<AlbumTrack> selectedTrack = idParam.id(req)
            .flatMap(media::getTrack)
            .map(track -> new AlbumTrack(albumContext, track));
        Stream<AlbumTrack> selectedAlbumStart = QPar.autoplay.id(req)
            .flatMap(media::getAlbumContext)
            .filter(albumContext::equals)
            .map(autoplayAlbum ->
                new AlbumTrack(
                    autoplayAlbum,
                    autoplayAlbum.getAlbum().getStartTrack()));
        return Stream.concat(selectedTrack, selectedAlbumStart)
            .filter(track ->
                StreamAuthorization.authorizedStreaming(media, req, track));
    }

    private static Optional<Track> previousTrack(Track track, Collection<Track> tracks) {
        if (track.getTrackNo() > 1) {
            return tracks.stream()
                .filter(t ->
                    Objects.equals(t.getPart(), track.getPart()))
                .filter(t ->
                    t.getTrackNo() == track.getTrackNo() - 1)
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
        Deque<Track> tracksInPart = tracks.stream()
            .filter(samePart(track))
            .sorted(trackNo())
            .collect(Collectors.toCollection(LinkedList::new));
        if (track.equals(tracksInPart.getLast())) {
            return nextPart(track, tracks).flatMap(nextPart -> tracks.stream()
                .filter(t ->
                    nextPart.equals(t.getPart()))
                .min(trackNo()));
        }
        return tracksInPart.stream()
            .filter(t ->
                t.getTrackNo() > track.getTrackNo())
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
        return parts(tracks)
            .filter(part ->
                part > track.getPart())
            .min(Integer::compareTo);
    }

    private static Optional<Integer> previousPart(Track track, Collection<Track> tracks) {
        return parts(tracks)
            .filter(part ->
                part < track.getPart())
            .max(Integer::compareTo);
    }

    private static Comparator<Track> trackNo() {
        return Comparator.comparing(Track::getTrackNo);
    }
}

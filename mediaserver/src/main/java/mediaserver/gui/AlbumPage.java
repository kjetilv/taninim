package mediaserver.gui;

import mediaserver.Config;
import mediaserver.GlobalState;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.Session;
import mediaserver.stream.StreamAuthorization;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mediaserver.sessions.AccessLevel.*;

public final class AlbumPage extends TemplateEnabled {

    private static final String M4A = "m4a";

    private static final String FLAC = "flac";

    private final Supplier<Media> media;

    public AlbumPage(Route route, Supplier<Media> media, Templater templater) {

        super(route, templater);
        this.media = media;
    }

    @Override
    protected Handling handle(Req req) {

        return template(req, media.get())
            .findFirst()
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Stream<Template> template(Req req, Media media) {

        return QPar.album.id(req)
            .flatMap(media::getAlbum)
            .map(album ->
                albumTemplate(media, req, album));
    }

    private Template albumTemplate(Media media, Req req, Album album) {

        Optional<SelectedTrack> selectedTrack = selectedTrack(media, req, album).findFirst();
        Optional<SelectedTrack> highlightedTrack = selectedTrack.filter(isHighlighted(req));

        return getTemplate(ALBUM_PAGE)
            .add(TPar.user, req.getSession().getActiveUser(req))
            .add(TPar.media, media)
            .add(TPar.plyr, Config.PLYR)
            .add(TPar.admin, req.getSession().getAccessLevel().satisfies(ADMIN))
            .add(TPar.album, album)
            .add(TPar.compressed, req.isLocal() && req.isFlac())
            .add(TPar.selected, selectedTrack)
            .add(TPar.trackHighlighted, highlightedTrack)
            .add(TPar.playableGroups, playableGroups(media, req, album))
            .add(TPar.albumPlayable, albumPlayable(media, req, album))
            .add(TPar.playlists, Playlist.playlistsWith(album))
            .add(TPar.curations, Playlist.curationsWith(album))
            .add(TPar.format, req.isLocal() ? FLAC : M4A);
    }

    private static Stream<SelectedTrack> selectedTrack(Media media, Req req, Album album) {

        return track(media, req, album, QPar.track)
            .flatMap(track -> selected(media, req, track));
    }

    private static Predicate<SelectedTrack> isHighlighted(Req req) {

        return selectedTrack ->
            Optional.ofNullable(selectedTrack.getTrack())
                .filter(st ->
                    GlobalState.get().getGlobalTrack(req.getTime()).map(Pair::getT2).filter(st::equals).isPresent())
                .isPresent();
    }

    private static Stream<SelectedTrack> selected(Media media, Req req, AlbumTrack albumTrack) {

        return Stream.of(req.getSession())
            .filter(session ->
                streamable(media, albumTrack, session))
            .map(session ->
                new SelectedTrack(
                    albumTrack,
                    autoplay(media, req, albumTrack),
                    previousTrack(albumTrack.getTrack(), albumTrack.getAlbum().getTracks())
                        .orElse(null),
                    nextTrack(albumTrack.getTrack(), albumTrack.getAlbum().getTracks())
                        .orElse(null)));
    }

    private static boolean streamable(Media media, AlbumTrack albumTrack, Session session) {

        return session.hasLevel(STREAM) || session.hasLevel(STREAM_CURATED) && media.isCurated(albumTrack);
    }

    private static boolean autoplay(Media media, Req req, AlbumTrack albumTrack) {

        return track(media, req, albumTrack.getAlbum(), QPar.autoplay)
            .anyMatch(albumTrack::equals);
    }

    private static Collection<PlayableGroup> playableGroups(Media media, Req req, Album album) {

        return playablesStream(media, req, album)
            .collect(Collectors.toList());
    }

    private static Optional<Playable> albumPlayable(Media media, Req req, Album album) {

        return playablesStream(media, req, album)
            .map(PlayableGroup::getPlayables)
            .flatMap(Collection::stream)
            .findFirst();
    }

    @NotNull
    private static Stream<PlayableGroup> playablesStream(Media media, Req req, Album album) {

        return album.getTracksByPart().entrySet().stream().map(e ->
            new PlayableGroup(
                e.getKey(),
                e.getValue().stream()
                    .map(t ->
                        new Playable(t, streamable(media, new AlbumTrack(album, t), req.getSession())))
                    .collect(Collectors.toList())));
    }

    private static Stream<AlbumTrack> track(Media media, Req req, Album album, QPar trackPar) {

        return trackPar.id(req)
            .flatMap(media::getTrack)
            .map(track ->
                new AlbumTrack(album, track))
            .filter(track ->
                StreamAuthorization.authorized(media, req, track));
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

        return t -> Objects.equals(track.getPart(), t.getPart());
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
}

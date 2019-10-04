package mediaserver.files;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Media {

    private final List<Album> library;

    public Media(Path root) {
        this.library = albums(root).collect(Collectors.toList());
    }

    public static Album album(Path path, File file, String artist, String name, List<Track> tracks) {
        return new Album(artist, name, tracks, file, new CategoryPath(path.getParent().getParent()));
    }

    public static Track track(String artist, String album, String name, File file) {
        return new Track(artist, album, name, file);
    }

    public Optional<Track> song(UUID uuid) {
        return songStream().filter(song -> song.getUuid().equals(uuid)).findFirst();
    }

    public Collection<CategoryPath> categories() {
        return library.stream().map(Album::getCategoryPath).collect(Collectors.toSet());
    }

    public Collection<String> albumArtists() {
        return library.stream().map(Album::getArtist).collect(Collectors.toSet());
    }

    public Collection<String> artists() {
        return library.stream()
            .map(Album::getTracks)
            .flatMap(Collection::stream)
            .map(Track::getArtist)
            .collect(Collectors.toSet());
    }

    public Collection<Album> albums() {
        return albumStream().collect(Collectors.toList());
    }

    public Collection<Track> songs() {
        return songStream().collect(Collectors.toList());
    }

    private Stream<Album> albumStream() {
        return library.stream().sorted();
    }

    private Stream<Track> songStream() {
        return albumStream().flatMap(album -> album.getTracks().stream());
    }

    private static Stream<Album> albums(Path path) {
        return files(path)
            .filter(File::isDirectory)
            .flatMap(dir ->
                Stream.concat(
                    album(subPath(path, dir), dir).stream(),
                    subDirs(dir).flatMap(subDir ->
                        albums(subPath(path, dir).resolve(subDir.getName())))));
    }

    private static Path subPath(Path path, File dir) {
        return path.resolve(dir.getName());
    }

    private static Optional<Album> album(Path path, File dir) {
        return trackFiles(dir).map(tracks ->
            album(
                path,
                dir,
                artistName(dir),
                albumName(dir),
                tracks(dir, tracks).collect(Collectors.toList())));
    }

    private static Stream<Track> tracks(File dir, File[] tracks) {
        return Arrays.stream(tracks)
            .map(track ->
                track(
                    artistName(dir),
                    albumName(dir),
                    trackName(track),
                    track));
    }

    private static Stream<File> subDirs(File dir) {
        return Optional.ofNullable(dir.listFiles()).stream().flatMap(Arrays::stream);
    }

    private static Optional<File[]> trackFiles(File dir) {
        return Optional.ofNullable(dir.listFiles())
            .filter(files ->
                files.length > 0 && Arrays.stream(files).allMatch(File::isFile));
    }

    private static String albumName(File dir) {
        return dir.getName();
    }

    private static String trackName(File track) {
        return track.getName();
    }

    private static String artistName(File dir) {
        return dir.getParentFile().getName();
    }

    private static Stream<File> files(Path path) {
        return subDirs(path.toFile());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + library.size() + " albums]";
    }
}

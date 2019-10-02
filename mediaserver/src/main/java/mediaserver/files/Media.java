package mediaserver.files;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Media {

    private static final Map<File, Track> tracks = new ConcurrentHashMap<>();

    private static final Map<File, Album> albums = new ConcurrentHashMap<>();

    private final List<Album> library;

    private final Path root;

    public Media(Path root) {
        this.root = root;
        this.library = traverse(root).collect(Collectors.toList());
    }

    public Track track(String artist, String album, String name, File file) {
        return track(artist, album, name, null, file);
    }

    public Album album(Path path, File file, String artist, String name, List<Track> tracks) {
        return albums.computeIfAbsent(
            file.getAbsoluteFile(),
            key ->
                new Album(artist, name, tracks, file, root.relativize(path.getParent().getParent())));
    }

    public static Track track(String artist, String album, String name, Integer part, File file) {
        return tracks.computeIfAbsent(
            file.getAbsoluteFile(),
            key ->
                new Track(artist, name, album, part, file));
    }

    private Stream<Album> traverse(Path path) {
        return files(path)
            .filter(File::isDirectory)
            .flatMap(dir ->
                Stream.concat(
                    album(subPath(path, dir), dir),
                    subDirs(dir)
                        .flatMap(subDir ->
                            traverse(subPath(path, dir).resolve(subDir.getName())))));
    }

    private Path subPath(Path path, File dir) {
        return path.resolve(dir.getName());
    }

    private Stream<Album> album(Path path, File dir) {
        return albumTracks(dir)
            .map(tracks ->
                album(
                    path,
                    dir,
                    artistName(dir),
                    albumName(dir),
                    Arrays.stream(tracks)
                        .map(track ->
                            track(
                                artistName(dir),
                                albumName(dir),
                                trackName(track),
                                track))
                        .collect(Collectors.toList())))
            .stream();
    }

    private Stream<File> subDirs(File dir) {
        return Optional.ofNullable(dir.listFiles()).stream()
            .flatMap(Arrays::stream);
    }

    private Optional<File[]> albumTracks(File dir) {
        return Optional.ofNullable(dir.listFiles())
            .filter(files ->
                files.length > 0 && Arrays.stream(files).allMatch(File::isFile));
    }

    private String albumName(File dir) {
        return dir.getName();
    }

    private String trackName(File track) {
        return track.getName();
    }

    private String artistName(File dir) {
        return dir.getParentFile().getName();
    }

    private Stream<File> files(Path path) {
        return subDirs(path.toFile());
    }
}

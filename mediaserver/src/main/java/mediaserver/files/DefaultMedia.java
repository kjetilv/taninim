package mediaserver.files;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMedia implements Media {

    private final CategoryPath categoryPath;

    private final List<Album> library;

    public DefaultMedia(Path root) {
        this(new CategoryPath(null), getAlbums(root, root));
    }

    private DefaultMedia(CategoryPath categoryPath, Stream<Album> albums) {
        this.categoryPath = categoryPath;
        this.library = albums.collect(Collectors.toList());
    }

    @Override
    public Media subLibrary(CategoryPath category) {
        if (category == null) {
            return this;
        }
        CategoryPath sub = categoryPath.sub(category);
        return new DefaultMedia(
            sub,
            stream(true).filter(album ->
                album.isIn(sub)));
    }

    @Override
    public Optional<Track> getSong(UUID uuid) {
        return songStream(true).filter(song -> song.getUuid().equals(uuid)).findFirst();
    }

    @Override
    public CategoryPath getCategoryPath() {
        return categoryPath;
    }

    @Override
    public Collection<CategoryPath> getTopCategories() {
        return categoryStream()
            .filter(cat ->
                !cat.equals(this.categoryPath))
            .map(cat ->
                cat.toTop(this.categoryPath))
            .flatMap(Optional::stream)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<CategoryPath> getCategories() {
        return categoryStream()
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getAlbumArtists(boolean recurse) {
        return stream(recurse).map(Album::getArtist).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getArtists(boolean recurse) {
        return stream(recurse)
            .map(Album::getTracks)
            .flatMap(Collection::stream)
            .map(Track::getArtist)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Album> getAlbums(boolean recurse) {
        return albumStream(recurse).collect(Collectors.toList());
    }

    @Override
    public Collection<Track> getSongs(boolean recurse) {
        return songStream(recurse).collect(Collectors.toList());
    }

    @Override
    public Optional<Album> getAlbum(UUID uuid) {
        return stream(true).filter(album -> album.getUuid().equals(uuid)).findFirst();
    }

    private Stream<Album> stream(boolean recurse) {
        return library.stream().filter(album ->
            recurse || album.getCategoryPath().equals(this.categoryPath));
    }

    private Stream<CategoryPath> categoryStream() {
        return stream(true).map(Album::getCategoryPath);
    }

    private Stream<Album> albumStream(boolean recurse) {
        return stream(recurse).sorted();
    }

    private Stream<Track> songStream(boolean recurse) {
        return albumStream(recurse).flatMap(album -> album.getTracks().stream());
    }

    private static Album album(Path root, Path path, File file, String artist, String name, List<Track> tracks) {
        return new Album(artist, name, tracks, file, new CategoryPath(root.relativize(path.getParent().getParent())));
    }

    private static Track track(String artist, String album, String name, File file) {
        return new Track(artist, album, name, file);
    }

    private static Stream<Album> getAlbums(Path root, Path path) {
        return Stream.concat(
            album(root, path).stream(),
            subDirs(path.toFile())
                .flatMap(subDir ->
                    getAlbums(root,
                        path.resolve(subDir.getName()))));
    }

    private static Optional<Album> album(Path root, Path path) {
        File dir = path.toFile();
        return trackFiles(dir).map(tracks ->
            album(
                root,
                path,
                dir,
                artistName(dir),
                albumName(dir),
                tracks(dir, tracks).collect(Collectors.toList())));
    }

    private static Stream<Track> tracks(File dir, List<File> tracks) {
        return tracks.stream()
            .map(track ->
                track(
                    artistName(dir),
                    albumName(dir),
                    trackName(track),
                    track));
    }

    private static Stream<File> subDirs(File dir) {
        return Optional.ofNullable(dir.listFiles(File::isDirectory))
            .stream()
            .flatMap(Arrays::stream);
    }

    private static Optional<List<File>> trackFiles(File dir) {
        return Optional.ofNullable(
            dir.listFiles(file ->
                file.isFile() && file.getName().endsWith(".flac")))
            .filter(files ->
                files.length > 0)
            .map(Arrays::asList);
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + library.size() + " albums]";
    }
}

package mediaserver.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mediaserver.util.IO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlaylistYaml {

    public static final ObjectReader YAML_READER =
        new ObjectMapper(new YAMLFactory()).readerFor(Map.class);

    public static final String PLAYLISTS_RESOURCE = "playlists.yaml";

    public static final Collection<PlaylistYaml> PLAYLISTS =
        categories(PLAYLISTS_RESOURCE);

    public static final String CURATED_RESOURCE = "curated.yaml";

    public static final Collection<PlaylistYaml> CURATED =
        categories(CURATED_RESOURCE);

    private final Path path;

    private final Collection<PlaylistEntry> entries;

    public PlaylistYaml(Path path, String... entries) {

        this(path, Arrays.asList(entries));
    }

    public PlaylistYaml(Path path, Collection<String> entries) {

        this(
            entries == null || entries.isEmpty()
                ? Collections.emptyList()
                : entries.stream().map(PlaylistEntry::new).collect(Collectors.toList()),
            Objects.requireNonNull(path));
    }

    private PlaylistYaml(Collection<PlaylistEntry> entries, Path path) {

        this.path = path;
        this.entries = entries;
    }

    public static PlaylistYaml combine(Path path, Collection<PlaylistYaml> subentries) {

        return new PlaylistYaml(
            subentries.stream().flatMap(sub -> sub.entries.stream()).collect(Collectors.toList()),
            path);
    }

    public static Collection<PlaylistYaml> categories(String resource) {
        return IO.read(resource)
            .unpack(value ->
                customCategories(null, readMap(resource, value))
                    .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);

        //        Collection<PlaylistYaml> combines =
//            customCategories.stream()
//                .flatMap(cc ->
//                    getSuperpaths(cc.getPath()))
//                .distinct()
//                .collect(Collectors.toMap(
//                    Function.identity(),
//                    path1 ->
//                        customCategories.stream()
//                            .filter(customCategory ->
//                                customCategory.isCovered(path1))
//                            .collect(Collectors.toList()))).entrySet().stream().map(e ->
//                combine(e.getKey(), e.getValue()))
//                .collect(Collectors.toList());
//        return Stream.of(customCategories, combines)
//            .flatMap(Collection::stream)
//            .sorted(Comparator.comparing(PlaylistYaml::getPath))
//            .collect(Collectors.toList());
    }

    public Path getPath() {

        return path;
    }

    public boolean contains(Album album) {

        return entries.stream().anyMatch(entry -> entry.match(album));
    }

    public boolean isCovered(Path path) {

        return entries.stream().anyMatch(entry -> entry.match(path));
    }

    public PlaylistYaml and(PlaylistYaml sub) {

        if (getPath() == null) {
            return sub;
        }
        if (sub.getPath().startsWith(getPath())) {
            return new PlaylistYaml(
                Stream.of(entries, sub.entries).flatMap(Collection::stream).distinct().collect(Collectors.toList()),
                getPath());
        }
        throw new IllegalArgumentException("Not a sub-category of " + this + ": " + sub);
    }

//    private static Stream<Path> getSuperpaths(Path path) {
//
//        return path.getParent() == null
//            ? Stream.empty()
//            : Stream.concat(Stream.of(path.getParent()), getSuperpaths(path.getParent()));
//    }

    private static Map<?, ?> readMap(String resource, String value) {

        try {
            return YAML_READER.readValue(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
    }

    private static Stream<PlaylistYaml> customCategories(Path prefix, Map<?, ?> map) {

        return map.entrySet().stream().flatMap(entry -> {
            Path path = Paths.get(String.valueOf(entry.getKey()));
            Path subPath = prefix == null ? path : prefix.resolve(path);
            if (entry.getValue() instanceof Collection<?>) {
                Optional<PlaylistYaml> level = Optional.of(entries((Collection<?>) entry.getValue()))
                    .filter(albums -> !albums.isEmpty())
                    .map(albums -> new PlaylistYaml(subPath, albums));
                Collection<PlaylistYaml> sublevel = subMaps((Collection<?>) entry.getValue()).stream()
                    .flatMap(sub ->
                        customCategories(subPath, sub))
                    .collect(Collectors.toList());
                return Stream.concat(
                    level.map(l -> sublevel.stream().reduce(l, PlaylistYaml::and, PlaylistYaml::and)).stream(),
                    sublevel.stream());
            }
            if (entry.getValue() instanceof Map<?, ?>) {
                return customCategories(subPath, (Map<?, ?>) entry.getValue());
            }
            return Stream.of(new PlaylistYaml(subPath, String.valueOf(entry.getKey())));
        });
    }

    @SuppressWarnings("unchecked")
    private static Collection<Map<?, ?>> subMaps(Collection<?> entries) {

        return (Collection<Map<?, ?>>) entries.stream()
            .filter(sub ->
                sub instanceof Map<?, ?>)
            .collect(Collectors.toList());
    }

    private static Collection<String> entries(Collection<?> entries) {

        return entries.stream()
            .filter(album -> !(album instanceof Map<?, ?>))
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + path + " (" + entries + ")]";
    }
}

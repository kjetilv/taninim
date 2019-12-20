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

public class CustomCategory {

    public static final ObjectReader YAML_READER =
        new ObjectMapper(new YAMLFactory()).readerFor(Map.class);

    private final Path path;

    private final Collection<PlaylistEntry> entries;

    public CustomCategory(Path path, String... entries) {

        this(path, Arrays.asList(entries));
    }

    public CustomCategory(Path path, Collection<String> entries) {

        this(
            entries == null || entries.isEmpty()
                ? Collections.emptyList()
                : entries.stream().map(PlaylistEntry::new).collect(Collectors.toList()),
            Objects.requireNonNull(path));
    }

    private CustomCategory(Collection<PlaylistEntry> entries, Path path) {

        this.path = path;
        this.entries = entries;
    }

    public static Collection<CustomCategory> categories(String resource) {

        return IO.read(resource).unpack()
            .map(value ->
                customCategories(null, readMap(resource, value))
                    .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    }

    public Path getPath() {

        return path;
    }

    public boolean contains(Album album) {

        return entries.stream().anyMatch(entry -> entry.albumMatch(album));
    }

    public boolean isCovered(Path path) {

        return containsAlbum(path.getParent().getFileName().toString());
    }

    public CustomCategory and(CustomCategory sub) {

        if (getPath() == null) {
            return sub;
        }
        if (sub.getPath().startsWith(getPath())) {
            return new CustomCategory(
                Stream.of(entries, sub.entries).flatMap(Collection::stream).distinct().collect(Collectors.toList()),
                getPath());
        }
        throw new IllegalArgumentException("Not a sub-category of " + this + ": " + sub);
    }

    private static Map<?, ?> readMap(String resource, String value) {

        try {
            return YAML_READER.readValue(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
    }

    private boolean containsAlbum(String name) {

        return entries.stream().anyMatch(entry -> entry.albumMatch(name));
    }

    private static Stream<CustomCategory> customCategories(Path prefix, Map<?, ?> map) {

        return map.entrySet().stream().flatMap(entry -> {
            Path path = Paths.get(String.valueOf(entry.getKey()));
            Path subPath = prefix == null ? path : prefix.resolve(path);
            if (entry.getValue() instanceof Collection<?>) {
                Optional<CustomCategory> level = Optional.of(entries((Collection<?>) entry.getValue()))
                    .filter(albums -> !albums.isEmpty())
                    .map(albums -> new CustomCategory(subPath, albums));
                Collection<CustomCategory> sublevel = subMaps((Collection<?>) entry.getValue()).stream()
                    .flatMap(sub ->
                        customCategories(subPath, sub))
                    .collect(Collectors.toList());
                return Stream.concat(
                    level.map(l -> sublevel.stream().reduce(l, CustomCategory::and, CustomCategory::and)).stream(),
                    sublevel.stream());
            }
            if (entry.getValue() instanceof Map<?, ?>) {
                return customCategories(subPath, (Map<?, ?>) entry.getValue());
            }
            return Stream.of(new CustomCategory(subPath, String.valueOf(entry.getKey())));
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

package mediaserver.media;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mediaserver.util.IO;

public final class PlaylistYaml {

    public static final String PLAYLISTS_RESOURCE = "playlists.yaml";

    public static final String CURATED_RESOURCE = "curated.yaml";

    public static Collection<PlaylistYaml> playlists(String resource) {
        return IO.readUTF8(resource)
            .unpack(value ->
                playlists(null, readMap(resource, value))
                    .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    }

    private final Path path;

    private final Collection<PlaylistEntry> entries;

    PlaylistYaml(Path path, String... entries) {
        this(path, Arrays.asList(entries));
    }

    private PlaylistYaml(Path path, Collection<String> entries) {
        this(
            entries == null || entries.isEmpty()
                ? Collections.emptyList()
                : entries.stream().map(PlaylistEntry::new).collect(Collectors.toList()),
            Objects.requireNonNull(path));
    }

    private PlaylistYaml(Collection<PlaylistEntry> entries, Path path) {
        this.path = path;
        this.entries = Objects.requireNonNull(entries, "entries");
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

    private PlaylistYaml and(PlaylistYaml sub) {
        if (sub == null) {
            return this;
        }
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

    private static final ObjectReader YAML_READER =
        new ObjectMapper(new YAMLFactory()).readerFor(Map.class);

    static final Collection<PlaylistYaml> PLAYLISTS =
        playlists(PLAYLISTS_RESOURCE);

    static final Collection<PlaylistYaml> CURATED =
        playlists(CURATED_RESOURCE);

    private static Map<?, ?> readMap(String resource, String value) {
        try {
            return YAML_READER.readValue(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
    }

    private static Stream<PlaylistYaml> playlists(@Nullable Path prefix, Map<?, ?> map) {
        return map.entrySet().stream().flatMap(entry -> {
            Path path = Paths.get(String.valueOf(entry.getKey()));
            Path subPath = prefix == null
                ? Objects.requireNonNull(path, "path")
                : prefix.resolve(path);
            if (entry.getValue() instanceof Collection<?>) {
                Optional<PlaylistYaml> level = Optional.of(entries((Collection<?>) entry.getValue()))
                    .filter(albums -> !albums.isEmpty())
                    .map(albums -> new PlaylistYaml(subPath, albums));
                Collection<PlaylistYaml> sublevel = subMaps((Collection<?>) entry.getValue()).stream()
                    .flatMap(sub ->
                        playlists(subPath, sub))
                    .collect(Collectors.toList());
                return Stream.concat(
                    level.map(lv ->
                        sublevel.stream().reduce(lv, PlaylistYaml::and, PlaylistYaml::and)).stream(),
                    sublevel.stream());
            }
            if (entry.getValue() instanceof Map<?, ?>) {
                return playlists(subPath, (Map<?, ?>) entry.getValue());
            }
            return Stream.of(new PlaylistYaml(subPath, String.valueOf(entry.getKey())));
        });
    }

    @SuppressWarnings("unchecked")
    private static Collection<Map<?, ?>> subMaps(Collection<?> entries) {
        return (Collection<Map<?, ?>>) entries.stream()
            .filter(isMap())
            .collect(Collectors.toList());
    }

    private static Collection<String> entries(Collection<?> entries) {
        return entries.stream()
            .filter(isMap().negate())
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    private static <T> Predicate<T> isMap() {
        return Map.class::isInstance;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + path + " (" + entries + ")]";
    }
}

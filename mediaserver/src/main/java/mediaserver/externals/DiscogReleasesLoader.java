package mediaserver.externals;

import mediaserver.util.IO;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class DiscogReleasesLoader {

    private final List<DiscogRelease> releases;

    public DiscogReleasesLoader(String ref, int start, int end) {

        List<DiscogRelease> releases = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            try {
                DiscogReleases x = IO.OM.readerFor(DiscogReleases.class).readValue(
                    Thread.currentThread().getContextClassLoader()
                        .getResource(ref.replace("x", String.valueOf(i))));
                releases.addAll(x.getReleases());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + ref, e);
            }
        }
        this.releases = releases;
    }

    public List<DiscogRelease> resources(String... query) {

        Collection<String> queryStream = split(query);
        return releases.stream()
            .filter(rel ->
                overlap(queryStream, rel) > query.length * 0.7)
            .sorted((rel1, rel2) ->
                -1 * Integer.compare(
                    overlap(queryStream, rel1),
                    overlap(queryStream, rel2)))
            .collect(Collectors.toList());
    }

    private int overlap(Collection<String> queryStream, DiscogRelease rel) {

        return overlap(queryStream, split(rel.getTitle()));
    }

    private int overlap(Collection<String> queryStream, Collection<String> titleParts) {

        return (int) Stream.concat(
            titleParts.stream(),
            queryStream.stream()
        ).distinct()
            .filter(queryStream::contains)
            .filter(titleParts::contains)
            .count();
    }

    private List<String> split(String... query) {

        Collection<String> stops = new HashSet<>(Arrays.asList("of", "a", "the", "vol", "volume"));
        Predicate<String> stopword = stops::contains;

        return stream(query)
            .flatMap(s ->
                Arrays.stream(s.split("\\s+")))
            .flatMap(s ->
                Arrays.stream(s.split(":+")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(stopword.negate())
            .map(String::toLowerCase)
            .map(part -> part.chars()
                .map(c -> Character.isLetterOrDigit(c) || c == '\'' ? c : ' ')
                .mapToObj(i -> Character.toString((char) i))
                .collect(Collectors.joining()))
            .flatMap(part ->
                part.endsWith("'s") ? Stream.of(part, part.substring(0, part.length() - 2))
                    : Stream.of(part))
            .collect(Collectors.toList());
    }
}

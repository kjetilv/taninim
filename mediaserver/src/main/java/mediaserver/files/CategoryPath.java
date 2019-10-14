package mediaserver.files;

import mediaserver.hash.AbstractHashable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CategoryPath extends AbstractHashable implements Comparable<CategoryPath> {

    public static final CategoryPath ROOT = new CategoryPath(Collections.emptyList());

    private final List<String> path;

    public CategoryPath() {
        this((Path)null);
    }

    public CategoryPath(Path path) {
        this(path == null ? Collections.emptyList() : parts(path));
    }

    private CategoryPath(List<String> path) {
        this.path = path == null || path.isEmpty() ? Collections.emptyList() : path;
    }

    public boolean startsWith(CategoryPath category) {
        if (category.isRoot()) {
            return true;
        }
        if (this.path.equals(category.getPath())) {
            return true;
        }
        if (this.path.size() >= category.path.size()) {
            List<String> prefix = this.path.subList(0, category.path.size());
            return prefix.equals(category.path);
        }
        return false;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, path.toArray(new String[path.size()]));
    }

    public String getPathString() {
        return isRoot() ? "/" : String.join("/", path);
    }

    public String getLastPathString() {
        return isRoot() ? "/" : path.get(path.size() - 1);
    }

    public String getFirstPathString() {
        return isRoot() ? "/" : path.get(0);
    }

    public List<String> getPath() {
        return path;
    }

    @Override
    public int compareTo(CategoryPath categoryPath) {
        return String.join("/", path).compareTo(String.join("/", categoryPath.getPath()));
    }

    public CategoryPath toTop() {
        return new CategoryPath(path.subList(0, 1));
    }

    public Optional<CategoryPath> toTop(CategoryPath prefix) {
        return Optional.of(this)
            .filter(me ->
                prefix.isRoot() || me.startsWith(prefix))
            .map(me ->
                new CategoryPath(path.subList(0, 1 + prefix.path.size())));
    }

    public CategoryPath sub(CategoryPath category) {
        return new CategoryPath(Stream.concat(
            this.path.stream(),
            category.path.stream()
        ).collect(Collectors.toUnmodifiableList()));
    }

    @Override
    public String toStringBody() {
        return String.join("/", path);
    }

    private boolean isRoot() {
        return path.isEmpty();
    }

    private static List<String> parts(Path path) {
        return IntStream.range(0, path.getNameCount())
            .mapToObj(path::getName)
            .map(Objects::toString)
            .collect(Collectors.toList());
    }
}

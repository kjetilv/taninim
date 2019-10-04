package mediaserver.files;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CategoryPath implements Comparable<CategoryPath> {

    private final List<String> path;

    public CategoryPath(Path path) {
        this.path = IntStream.range(0, path.getNameCount())
            .mapToObj(path::getName)
            .map(Objects::toString)
            .collect(Collectors.toList());
    }

    public List<String> getPath() {
        return path;
    }

    @Override
    public int compareTo(CategoryPath categoryPath) {
        return String.join("/", path).compareTo(String.join("/", categoryPath.getPath()));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CategoryPath && Objects.equals(path, ((CategoryPath) o).path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}

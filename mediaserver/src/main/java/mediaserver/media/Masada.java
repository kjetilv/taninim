package mediaserver.media;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Masada {

    private final Collection<MasadaBook> books;

    private final Map<String, MasadaRef> refs;

    public Masada(int books) {

        this.books = IntStream.range(0, books)
            .mapToObj(book ->
                new MasadaBook(book + 1))
            .collect(Collectors.toList());

        this.refs = this.books.stream()
            .flatMap(MasadaBook::refs)
            .collect(Collectors.toMap(
                masadaRef ->
                    masadaRef.getName().toLowerCase(),
                Function.identity()));
    }

    public Collection<MasadaBook> getBooks() {

        return books;
    }

    public Optional<MasadaRef> getMasadaRef(String name) {
        return Optional.ofNullable(name)
            .map(String::toLowerCase)
            .map(this.refs::get);
    }

}

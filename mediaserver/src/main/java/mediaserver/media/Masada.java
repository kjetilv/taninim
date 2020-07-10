package mediaserver.media;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;

import com.google.common.base.Functions;

public class Masada {

    private final Collection<MasadaBook> books;

    public Masada(int books) {
        this(IntStream.range(0, books)
            .mapToObj(book ->
                new MasadaBook(book + 1))
            .collect(Collectors.toList()));
    }

    private Masada(Collection<MasadaBook> books) {
        this.books = books;
    }

    public Collection<MasadaBook> getBooks() {
        return books;
    }

    public Masada withNotes(Collection<MasadaRef> refs) {
        return new Masada(books.stream()
            .map(book ->
                book.withNodes(notedRefs(book.getBook(), refs)))
            .collect(Collectors.toList()));
    }

    @Nonnull
    public static Map<Integer, MasadaRef> notedRefs(int book, Collection<MasadaRef> refs) {
        return refs.stream()
            .filter(ref -> ref.getBook() == book)
            .collect(Collectors.toMap(
                MasadaRef::getNumber, Functions.identity()
            ));
    }
}

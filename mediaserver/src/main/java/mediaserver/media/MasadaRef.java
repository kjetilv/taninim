package mediaserver.media;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import mediaserver.hash.AbstractNameHashable;

public class MasadaRef extends AbstractNameHashable {

    private final int book;

    private final int number;

    private final Collection<String> notes;

    public MasadaRef(int book, int number, String name) {
        this(book, number, name, null);
    }

    public MasadaRef(int book, int number, String name, Collection<String> notes) {
        super(name);
        this.book = book;
        this.number = number;
        this.notes = notes == null || notes.isEmpty() ? Collections.emptyList() : List.copyOf(notes);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append("Book ").append(book)
            .append(" #").append(number)
            .append(" ").append(getName())
            .append(" ").append(notes);
    }

    public int getBook() {
        return book;
    }

    public int getNumber() {
        return number;
    }

    @Serial private static final long serialVersionUID = 6109235301263559725L;
}

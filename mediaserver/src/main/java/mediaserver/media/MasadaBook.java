package mediaserver.media;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.IO;

import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MasadaBook extends AbstractHashable {

    private final int book;

    private static final Pattern WS = Pattern.compile("\\s+");

    public MasadaBook(int book) {

        this.book = book;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, book);
    }

    public int getBook() {

        return book;
    }

    public Stream<MasadaRef> refs() {

        return IO.readLines("Book " + book + ".csv")
            .unpack(lines ->
                lines.map(this::masadaRef))
            .orElseGet(Stream::empty);
    }

    private MasadaRef masadaRef(String line) {

        String[] split = WS.split(line, 2);
        int no = Integer.parseInt(split[0]);
        String name = split[1];
        return new MasadaRef(book, no, name);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append("Book ").append(book);
    }
}

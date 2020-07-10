package mediaserver.media;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.IO;

public class MasadaBook extends AbstractHashable {

    private final int book;

    private final Collection<MasadaRef> refs;

    public MasadaBook(int book) {
        this(book, refs(book).collect(Collectors.toList()));
    }

    public MasadaBook(int book, Collection<MasadaRef> refs) {
        this.book = book;
        this.refs = refs;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, book);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append("Book ").append(book);
    }

    public int getBook() {
        return book;
    }

    public MasadaBook withNodes(Map<Integer, MasadaRef> notedRefs) {
        Collection<Integer> numbers = refs.stream().mapToInt(MasadaRef::getNumber).boxed().collect(Collectors.toList());
        Collection<Integer> notedNumbers = notedRefs.keySet();
        if (numbers.size() == notedNumbers.size() && numbers.containsAll(notedNumbers)) {
            return new MasadaBook(book, notedRefs.values());
        }
        return null;
    }

    private static Stream<MasadaRef> refs(int book) {
        return IO.readLines("Book" + book + ".csv")
            .unpack(lines ->
                lines.map(line -> masadaRef(book, line)))
            .orElseGet(Stream::empty);
    }

    private static MasadaRef masadaRef(int book, String line) {
        String[] split = WS.split(line, 2);
        int no = Integer.parseInt(split[0]);
        String name = split[1];
        return new MasadaRef(book, no, name);
    }

    private static final Pattern WS = Pattern.compile("\\s+");
}

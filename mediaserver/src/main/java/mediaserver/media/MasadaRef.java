package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

public class MasadaRef extends AbstractNameHashable {

    private final int book;

    private final int number;

    private static final long serialVersionUID = 6109235301263559725L;

    MasadaRef(int book, int number, String name) {

        super(name);
        this.book = book;
        this.number = number;
    }

    public int getBook() {

        return book;
    }

    public int getNumber() {

        return number;
    }
}

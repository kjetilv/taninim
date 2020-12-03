package mediaserver.media;

import java.io.Serial;

import mediaserver.hash.AbstractNameHashable;

public final class Series extends AbstractNameHashable {

    public static Series get(String name) {
        return AbstractNameHashable.get(Series::new, name);
    }

    private Series(String name) {
        super(name);
    }

    @Serial private static final long serialVersionUID = 8396940978009264692L;
}

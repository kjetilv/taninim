package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

public final class Series extends AbstractNameHashable {

    private static final long serialVersionUID = 8396940978009264692L;

    private Series(String name) {

        super(name);
    }

    public static Series get(String name) {

        return AbstractNameHashable.get(Series::new, name);
    }
}

package mediaserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

public final class R {

    static final Random RANDOM = new Random();

    private R() {

    }

    public static <T> Optional<T> nd(Collection<T> ts) {

        return ts == null || ts.isEmpty()
            ? Optional.empty()
            : Optional.of(new ArrayList<>(ts).get(RANDOM.nextInt(ts.size())));
    }
}

package taninim.util;

import module java.base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Sys {

    private static final Logger log = LoggerFactory.getLogger(Sys.class);

    public static void atShutdown(Runnable action) {
        AT_SHUTDOWN.add(action);
    }

    public static void logTimeSinceStartup(Consumer<Duration> consumer) {
        consumer.accept(Time.sinceStart());
    }

    public static void logTimeAtShutdown(Consumer<Duration> consumer) {
        atShutdown(() -> consumer.accept(Time.sinceStart()));
    }

    private Sys() {
    }

    private static final List<Runnable> AT_SHUTDOWN = new CopyOnWriteArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () ->
                    AT_SHUTDOWN.forEach(action -> {
                        try {
                            action.run();
                        } catch (Exception e) {
                            log.warn("Shutdown hook failed...", e);
                        }
                    }),
                Sys.class.getSimpleName().toLowerCase(Locale.ROOT) + "-shutdown"
            )
        );
    }

}

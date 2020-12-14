package mediaserver;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;

public final class Config {

    public static final ZoneId TIMEZONE = ZoneId.of(setting("timeZone").orElse("CET"));

    public static final int K = 1024;

    public static final boolean PLYR = set("plyr");

    static final boolean PRETEND_SSL = set("ssl");

    static final boolean DEV_LOGIN = set("dev");

    static final int PORT = PRETEND_SSL ? 1443 : DEV_LOGIN ? 1080 : 80;

    static final Duration SESSION_LENGTH = duration("sessionLength", Duration.ofDays(1));

    static final Duration INACTIVITY_MAX = duration("inactivityMax", Duration.ofHours(1));

    static final int LISTEN_GROUP = count("listenGroup", Runtime.getRuntime().availableProcessors());

    static final int WORK_GROUP = count("workGroup", 16);

    static final int THREAD_GROUP = count("threadGroup", 32);

    static final int THREAD_QUEUE = count("threadQueue", 32);

    static final Duration TIMEOUT = duration("connectTimeout", Duration.ofSeconds(30));

    static final Duration REFRESH_TIME = duration("refresh", Duration.ofMinutes(3));

    private Config() {

    }

    private static final int MEGAS_PER_SESSION = count("sessionMb", 256);

    /**
     * Set to 0 to chunk according to requests.
     */
    private static final int MEGAS_PER_CHUNK = count("chunkMb", 16);

    private static final int KILOS_PER_SESSION = MEGAS_PER_SESSION * K;

    static final int BYTES_PER_SESSION = KILOS_PER_SESSION * K;

    private static final int KILOS_PER_CHUNK = MEGAS_PER_CHUNK * K;

    static final int BYTES_PER_CHUNK = KILOS_PER_CHUNK * K;

    private static final boolean LIVE = set("live");

    static final boolean NEUTER = !LIVE && !DEV_LOGIN;

    private static Duration duration(String setting, Duration defaultDuration) {
        return setting(setting)
            .map(Duration::parse)
            .orElse(defaultDuration);
    }

    private static Optional<String> setting(String setting) {
        return Optional.ofNullable(System.getProperty(setting, System.getenv(setting)));
    }

    private static Integer count(String setting, int defaultCount) {
        return setting(setting)
            .map(Integer::parseInt)
            .orElse(defaultCount);
    }

    private static boolean set(String flag) {
        return Boolean.getBoolean(flag) ||
            Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
    }
}

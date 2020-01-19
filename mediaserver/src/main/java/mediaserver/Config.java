package mediaserver;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;

public final class Config {

    public static final ZoneId TIMEZONE = ZoneId.of(setting("timeZone").orElse("CET"));

    public static final Duration SESSION_LENGTH = duration("sessionLength", Duration.ofDays(1));

    public static final Duration INACTIVITY_MAX = duration("inactivityMax", Duration.ofHours(1));

    public static final int MEGAS_PER_SESSION = count("sessionMb", 255);

    /**
     * Set to 0 to chunk according to requests.
     */
    public static final int KILOS_PER_CHUNK = count("chunkKb", 256);

    public static final int LISTEN_GROUP = count("listenGroup", Runtime.getRuntime().availableProcessors());

    public static final int WORK_GROUP = count("workGroup", 16);

    public static final int THREAD_GROUP = count("threadGroup", 32);

    public static final int THREAD_QUEUE = count("threadQueue", 32);

    public static final int K = 1024;

    public static final int KILOS_PER_SESSION = MEGAS_PER_SESSION * K;

    public static final int BYTES_PER_SESSION = KILOS_PER_SESSION * K;

    public static final int BYTES_PER_CHUNK = KILOS_PER_CHUNK * K;

    public static final Duration IO_TIMEOUT = duration("timeout", Duration.ofMinutes(1));

    public static final Duration CONNECT_TIMEOUT = duration("connectTimeout", Duration.ofSeconds(30));

    public static final Duration REFRESH_TIME = duration("refresh", Duration.ofMinutes(3));

    public static final boolean PLYR = set("plyr");

    public static final boolean LIVE = set("live");

    public static final boolean DEV_LOGIN = set("dev");

    public static final boolean NEUTER = !(LIVE || DEV_LOGIN);

    public static final boolean PRETEND_SSL = set("ssl");

    public static final int PORT = PRETEND_SSL ? 1443
        : DEV_LOGIN ? 1080
        : 80;

    private Config() {

    }

    private static Duration duration(String setting, Duration defaultDuration) {

        return setting(setting)
            .map(Duration::parse)
            .orElse(defaultDuration);
    }

    private static Integer count(String setting, int defaultCount) {

        return setting(setting)
            .map(Integer::parseInt)
            .orElse(defaultCount);
    }

    private static Optional<String> setting(String setting) {

        return Optional.ofNullable(System.getProperty(setting, System.getenv(setting)));
    }

    private static boolean set(String flag) {

        return Boolean.getBoolean(flag) ||
            Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
    }
}

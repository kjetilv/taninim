package mediaserver;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;

public class Config {

    public static final ZoneId TIMEZONE = ZoneId.of(setting("timeZone").orElse("CET"));

    public static final Duration SESSION_LENGTH = duration("sessionLength", Duration.ofDays(1));

    public static final Duration INACTIVITY_MAX = duration("inactivityMax", Duration.ofHours(1));

    public static final int MEGAS_PER_SESSION = count("sessionMb", 255);

    public static final int KILOS_PER_SESSION = MEGAS_PER_SESSION * 1024;

    public static final int BYTES_PER_SESSION = KILOS_PER_SESSION * 1024;

    static final Duration REFRESH_TIME = duration("refresh", Duration.ofMinutes(5));

    static final boolean LIVE = isTrue("live");

    static final boolean DEV_LOGIN = isTrue("dev");

    static final boolean NEUTER = !(LIVE || DEV_LOGIN);

    static final boolean PRETEND_SSL = isTrue("ssl");

    static final int PORT = PRETEND_SSL ? 1443
        : DEV_LOGIN ? 1080
        : 80;

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

    private static boolean isTrue(String flag) {

        return Boolean.getBoolean(flag) ||
            Optional.ofNullable(System.getenv(flag)).filter("true"::equals).isPresent();
    }
}

package mediaserver.util;

import mediaserver.Config;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class Print {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int K = 1_024;

    public static final int M = K * K;

    private Print() {

    }

    public static String aboutTime(Instant instant) {

        return instant.atZone(Config.TIMEZONE)
            .truncatedTo(ChronoUnit.MINUTES)
            .format(DATE_TIME_FORMATTER);
    }

    public static String pretty(Duration dur) {

        if (dur.minus(Duration.ofHours(1)).isNegative()) {
            return String.format(
                "%d:%02d",
                dur.toMinutesPart(),
                dur.toSecondsPart());
        }
        if (dur.minus(Duration.ofDays(1)).isNegative()) {
            return String.format(
                "%d time%s og %d minutt%s",
                dur.toHoursPart(),
                dur.toHoursPart() > 1 ? "r" : "",
                dur.toMinutesPart(),
                dur.toMinutesPart() > 1 ? "er" : "");
        }
        return String.format(
            "%d dag%s, %d time%s og %d minutt%s",
            dur.toDaysPart(),
            dur.toDaysPart() > 1 ? "er" : "",
            dur.toHoursPart(),
            dur.toHoursPart() > 1 ? "r" : "",
            dur.toMinutesPart(),
            dur.toMinutesPart() > 1 ? "er" : "");
    }

    public static String bytes(long bytes) {

        if (bytes > M) {
            return bytes / M + "mb";
        }
        if (bytes > K) {
            return bytes / K + "kb";
        }
        return bytes + "b";
    }
}

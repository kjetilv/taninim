package mediaserver.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import mediaserver.Config;

public final class Print {

    public static String aboutTime(Instant instant) {
        return instant.atZone(Config.TIMEZONE)
            .truncatedTo(ChronoUnit.MINUTES)
            .format(DATE_TIME_FORMATTER);
    }

    public static String uuid(UUID uuid) {
        String s = uuid.toString();
        int endIndex = s.indexOf('-');
        return endIndex < 0 ? s : s.substring(0, endIndex);
    }

    public static String prettyTrackTime(Duration dur) {
        if (dur.minus(Duration.ofHours(1)).isNegative()) {
            return String.format(
                "%02d:%02d",
                dur.toMinutesPart(),
                dur.toSecondsPart());
        }
        return String.format(
            "%d:%02d:%02d",
            dur.toHoursPart(),
            dur.toMinutesPart(),
            dur.toSecondsPart());
    }

    public static String prettyLongTime(Duration dur) {
        int min = dur.toMinutesPart();
        if (dur.minus(Duration.ofHours(1)).isNegative()) {
            return String.format(
                "%d minutt%s",
                min, mul(min, "er"));
        }
        int hors = dur.toHoursPart();
        if (dur.minus(Duration.ofDays(1)).isNegative()) {
            if (min == 0) {
                return String.format(
                    "%d time%s",
                    hors, mul(hors, "r"));
            }
            return String.format(
                "%d time%s og %d minutt%s",
                hors, mul(hors, "r"),
                min, mul(min, "er"));
        }
        long days = dur.toDaysPart();
        if (min == 0) {
            if (hors == 0) {
                return String.format(
                    "%d dag%s",
                    days, mul(days, "er"));
            }
            return String.format(
                "%d dag%s og %d time%s",
                days, days > 1 ? "er" : "",
                hors, mul(hors, "r"));
        }
        if (hors == 0) {
            return String.format(
                "%d dag%s, og %d minutt%s",
                days, days > 1 ? "er" : "",
                min, mul(min, "er"));
        }
        return String.format(
            "%d dag%s, %d time%s og %d minutt%s",
            days, days > 1 ? "er" : "",
            hors, mul(hors, "r"),
            min, mul(min, "er"));
    }

    @SuppressWarnings("MagicNumber")
    public static String bytes(long bytes) {
        if (bytes > 10 * M) {
            return String.format("%dMb", bytes / M);
        }
        if (bytes > M) {
            return String.format("%.1fMb", bytes * 10 / M / 10.0D);
        }
        if (bytes > K) {
            return String.format("%dKb", bytes / K);
        }
        return String.format("%db", bytes);
    }

    private Print() {
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int K = 1_024;

    private static final int M = K * K;

    private static String mul(int min, String er) {
        return mul((long) min, er);
    }

    private static String mul(long min, String er) {
        return min > 1 ? er : "";
    }
}

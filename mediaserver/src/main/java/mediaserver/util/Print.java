package mediaserver.util;

import mediaserver.Main;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class Print {

    Print() {

    }

    public static String aboutTime(Instant instant) {
        return instant.atZone(Main.TIMEZONE).format(DateTimeFormatter.ISO_DATE_TIME);
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
}

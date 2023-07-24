package taninim.music;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public record Period(
    Instant start,
    Duration duration
) {

    public static Period starting(Instant start) {
        return new Period(start);
    }

    public Period {
        requireNonNull(start, "start");
    }

    public Period(Instant start) {
        this(requireNonNull(start, "start"), null);
    }

    public Period ofLength(Duration duration) {
        return new Period(start, requireNonNull(duration, "duration"));
    }

    public Period capTo(Duration duration) {
        return starting(start)
            .ofLength(cappedDuration(requireNonNull(duration, "duration")));
    }

    public long epochHour() {
        return start.getEpochSecond() / SECONDS_PR_HOUR;
    }

    public Stream<Long> epochHoursBack(Duration interval) {
        long startHour = starting(start.minus(interval)).epochHour();
        return LongStream.range(startHour, epochHour() + 1L).boxed();
    }

    public Instant getLapse() {
        if (duration == null) {
            throw new IllegalStateException(this + " has no duration");
        }
        return start.plus(duration);
    }

    private Duration cappedDuration(Duration limit) {
        return Optional.ofNullable(duration)
            .flatMap(wanted ->
                earliest(limit, wanted))
            .orElse(limit);
    }

    private static final long SECONDS_PR_HOUR = Duration.ofHours(1).toSeconds();

    private static Optional<Duration> earliest(Duration leaseLimit, Duration leaseDuration) {
        return Stream.of(leaseDuration, leaseLimit).min(Comparator.naturalOrder());
    }
}

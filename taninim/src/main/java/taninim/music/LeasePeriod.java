package taninim.music;

import module java.base;

import static java.util.Objects.requireNonNull;

public record LeasePeriod(Instant start, Duration duration) {

    public static LeasePeriod starting(Instant start) {
        return new LeasePeriod(start);
    }

    public LeasePeriod {
        requireNonNull(start, "start");
    }

    public LeasePeriod(Instant start) {
        this(requireNonNull(start, "start"), null);
    }

    public LeasePeriod ofLength(Duration duration) {
        return new LeasePeriod(start, requireNonNull(duration, "duration"));
    }

    public LeasePeriod capTo(Duration duration) {
        return starting(start)
            .ofLength(cappedDuration(requireNonNull(duration, "duration")));
    }

    public long epochHour() {
        return start.getEpochSecond() / SECONDS_PR_HOUR;
    }

    public Stream<Long> epochHoursBack(Duration interval) {
        var startHour = starting(start.minus(interval)).epochHour();
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

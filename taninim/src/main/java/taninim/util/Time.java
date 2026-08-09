package taninim.util;

import module java.base;

import java.lang.management.ManagementFactory;

public final class Time {

    public static Duration sinceStart() {
        var startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        var now = System.currentTimeMillis();
        return Duration.between(
            Instant.ofEpochMilli(startTime),
            Instant.ofEpochMilli(now)
        );
    }

    private Time() {
    }
}

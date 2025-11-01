package taninim;

import module java.base;

public record TaninimSettings(
    Duration sessionDuration,
    Duration leaseDuration,
    int transferSize
) {
}

package taninim;

import java.time.Duration;

public record TaninimSettings(
    Duration sessionDuration,
    Duration leaseDuration,
    int transferSize
) {

}

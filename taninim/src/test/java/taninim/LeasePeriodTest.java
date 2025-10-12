package taninim;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import taninim.music.LeasePeriod;

import static org.assertj.core.api.Assertions.assertThat;

class LeasePeriodTest {

    @Test
    void should_accept_cap() {
        var length = new LeasePeriod(Instant.EPOCH, Duration.ofHours(4))
            .capTo(Duration.ofHours(1)).duration();
        assertThat(length).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void shouldProduceEpochHour() {
        var leasePeriod = LeasePeriod.starting(Instant.EPOCH.plus(Duration.ofHours(10)));
        assertThat(leasePeriod.epochHour()).isEqualTo(10);
    }

    @Test
    void shouldProduceEpochHoursBack() {
        var leasePeriod = LeasePeriod.starting(Instant.EPOCH.plus(Duration.ofHours(10)));
        assertThat(leasePeriod.epochHoursBack(
            Duration.ofHours(5))).containsExactly(5L, 6L, 7L, 8L, 9L, 10L);
    }
}

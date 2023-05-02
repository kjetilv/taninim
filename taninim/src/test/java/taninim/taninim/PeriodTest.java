package taninim.taninim;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import taninim.taninim.music.Period;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodTest {

    @Test
    void should_accept_cap() {
        Duration length = new Period(Instant.EPOCH, Duration.ofHours(4))
            .capTo(Duration.ofHours(1)).duration();
        assertThat(length).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void shouldProduceEpochHour() {
        Period period = Period.starting(Instant.EPOCH.plus(Duration.ofHours(10)));
        assertThat(period.epochHour()).isEqualTo(10);
    }

    @Test
    void shouldProduceEpochHoursBack() {
        Period period = Period.starting(Instant.EPOCH.plus(Duration.ofHours(10)));
        assertThat(period.epochHoursBack(
            Duration.ofHours(5))).containsExactly(5L, 6L, 7L, 8L, 9L, 10L);
    }
}

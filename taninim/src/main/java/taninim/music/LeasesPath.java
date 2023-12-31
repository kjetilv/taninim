package taninim.music;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;

import com.github.kjetilv.uplift.uuid.Uuid;

public record LeasesPath(
    Leases leases,
    Period period
) {

    public String toPath() {
        return MessageFormat.format(
            "{0}{1,number,00000000}-{2}{3}",
            LEASE_PREFIX,
            period.epochHour(),
            leases.token().digest(),
            LEASE_SUFFIX
        );
    }

    public LeasesPath withTracks(List<? extends Uuid> uuids, Instant lapse) {
        return new LeasesPath(leases.withTracks(uuids, lapse), period);
    }

    private static final String LEASE_PREFIX = "lease-";

    private static final String LEASE_SUFFIX = ".lease.txt";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leases + "/ " + period + " @ " + toPath() + "]";
    }
}

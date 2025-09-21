package taninim.music;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;

import com.github.kjetilv.uplift.uuid.Uuid;

public record LeasesPath(
    Leases leases,
    LeasePeriod leasePeriod
) {

    public String toPath() {
        return MessageFormat.format(
            "{0}{1,number,00000000}-{2}{3}",
            LEASE_PREFIX,
            leasePeriod.epochHour(),
            leases.token().digest(),
            LEASE_SUFFIX
        );
    }

    public LeasesPath withTracks(List<? extends Uuid> uuids, Instant lapse) {
        return new LeasesPath(leases.withTracks(uuids, lapse), leasePeriod);
    }

    private static final String LEASE_PREFIX = "lease-";

    private static final String LEASE_SUFFIX = ".lease.txt";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leases + "/ " + leasePeriod + " @ " + toPath() + "]";
    }
}

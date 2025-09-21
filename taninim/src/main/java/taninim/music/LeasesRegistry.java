package taninim.music;

import module java.base;
import module uplift.uuid;

public interface LeasesRegistry {

    Optional<LeasesPath> getActive(Uuid token);

    LeasesPath setActive(Leases leases, LeasePeriod leasePeriod);
}

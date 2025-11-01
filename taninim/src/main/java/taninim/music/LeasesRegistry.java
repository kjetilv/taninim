package taninim.music;

import module java.base;
import com.github.kjetilv.uplift.uuid.Uuid;

public interface LeasesRegistry {

    Optional<LeasesPath> getActive(Uuid token);

    LeasesPath setActive(Leases leases, LeasePeriod leasePeriod);
}

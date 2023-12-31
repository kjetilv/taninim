package taninim.music;

import java.util.Optional;

import com.github.kjetilv.uplift.uuid.Uuid;

public interface LeasesRegistry {

    Optional<LeasesPath> getActive(Uuid token);

    LeasesPath setActive(Leases leases, Period period);
}

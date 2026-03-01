package taninim.music;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import taninim.auth.Authed;

public interface LeasesRegistry {

    Authed<LeasesPath> getActive(Hash<HashKind.K128> token);

    LeasesPath setActive(Leases leases, LeasePeriod leasePeriod);
}

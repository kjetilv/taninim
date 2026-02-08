package taninim.yellin;

import module java.base;
import taninim.fb.ExtAuthResponse;

public interface Yellin {

    Optional<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> createLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> requestLease(LeasesRequest leasesRequest);

    Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest);
}

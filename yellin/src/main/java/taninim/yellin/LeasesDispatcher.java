package taninim.yellin;

import module java.base;
import module taninim.fb;

public interface LeasesDispatcher {

    Optional<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> createLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> requestLease(LeasesRequest leasesRequest);

    Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest);
}

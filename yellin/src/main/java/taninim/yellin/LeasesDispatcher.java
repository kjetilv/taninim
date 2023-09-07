package taninim.yellin;

import java.util.Optional;

import taninim.fb.ExtAuthResponse;

public interface LeasesDispatcher {

    Optional<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> createLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> requestLease(LeasesRequest leasesRequest);

    Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest);
}

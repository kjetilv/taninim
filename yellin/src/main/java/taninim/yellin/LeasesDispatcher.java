package taninim.yellin;

import taninim.fb.ExtAuthResponse;

import java.util.Optional;

public interface LeasesDispatcher {

    Optional<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> createLease(ExtAuthResponse extAuthResponse);

    Optional<LeasesActivation> requestLease(LeasesRequest leasesRequest);

    Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest);
}

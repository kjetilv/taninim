package taninim.yellin;

import java.util.Optional;

import taninim.fb.ExtAuthResponse;

public interface LeasesDispatcher {

    default Optional<LeasesActivationResult> currentLease(ExtAuthResponse extAuthResponse) {
        return currentLease(extAuthResponse, false);
    }

    default Optional<LeasesActivationResult> createLease(ExtAuthResponse extAuthResponse) {
        return currentLease(extAuthResponse, true);
    }

    Optional<LeasesActivationResult> currentLease(ExtAuthResponse extAuthResponse, boolean refresh);

    Optional<LeasesActivationResult> requestLease(LeasesRequest leasesRequest);

    Optional<LeasesActivationResult> dismissLease(LeasesRequest leasesRequest);
}

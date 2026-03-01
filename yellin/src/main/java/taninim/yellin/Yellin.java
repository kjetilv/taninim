package taninim.yellin;

import module java.base;
import taninim.auth.Authed;
import taninim.fb.ExtAuthResponse;

public interface Yellin {

    Authed<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse);

    Authed<LeasesActivation> createLease(ExtAuthResponse extAuthResponse);

    Authed<LeasesActivation> requestLease(LeasesRequest leasesRequest);

    Authed<LeasesActivation> dismissLease(LeasesRequest leasesRequest);
}

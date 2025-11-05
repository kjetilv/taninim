package taninim.fb;

import module java.base;

@FunctionalInterface
public interface Authenticator {

    Optional<ExtUser> authenticate(ExtAuthResponse authResponse);
}

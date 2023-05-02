package taninim.fb;

import java.util.Optional;

@FunctionalInterface
public interface Authenticator {

    Optional<ExtUser> authenticate(ExtAuthResponse authResponse);
}

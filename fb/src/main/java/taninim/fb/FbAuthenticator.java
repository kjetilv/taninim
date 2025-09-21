package taninim.fb;

import java.util.Optional;

@FunctionalInterface
public interface FbAuthenticator {

    static FbAuthenticator simple() {
        return new DefaultFbAuthenticator();
    }

    Optional<ExtUser> authenticate(ExtAuthResponse authResponse);
}

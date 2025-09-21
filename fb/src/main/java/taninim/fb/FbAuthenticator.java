package taninim.fb;

import module java.base;

@FunctionalInterface
public interface FbAuthenticator {

    static FbAuthenticator simple() {
        return new DefaultFbAuthenticator();
    }

    Optional<ExtUser> authenticate(ExtAuthResponse authResponse);
}

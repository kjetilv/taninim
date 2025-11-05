package taninim.fb;

import module java.base;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultFbAuthenticator implements Authenticator {

    private static final Logger log = LoggerFactory.getLogger(DefaultFbAuthenticator.class);

    private final Supplier<char[]> appSecret = FbSec.secretsProvider();

    @Override
    public Optional<ExtUser> authenticate(ExtAuthResponse authResponse) {
        var id = authResponse.userID();
        try {
            log.debug("Looking up {}", authResponse);
            return getExtUser(authResponse, id).map(remoteUser -> {
                log.debug("Retrieved user {}/{}, {}", remoteUser, remoteUser.id(), authResponse);
                if (remoteUser.hasId(id)) {
                    return remoteUser;
                }
                log.debug("Disallowed {}: {}", authResponse, remoteUser);
                return null;
            });
        } catch (Exception e) {
            throw new IllegalStateException("Login failed for user: " + id, e);
        }
    }

    private FacebookClient facebookClient(ExtAuthResponse authResponse) {
        return new DefaultFacebookClient(
            authResponse.accessToken(),
            new String(appSecret.get()),
            new SimpleRequestor(),
            new SimpleMapper(),
            Version.LATEST
        );
    }

    private Optional<ExtUser> getExtUser(ExtAuthResponse authResponse, String id) {
        return Optional.of(authResponse)
            .map(this::facebookClient)
            .map(facebookClient -> {
                try {
                    return facebookClient.fetchObject(id, ExtUser.class);
                } catch (Exception e) {
                    if (e.getMessage().toLowerCase(Locale.ROOT).contains("has expired")) {
                        if (log.isDebugEnabled()) {
                            log.debug("Expired auth: {}", authResponse, e);
                        } else {
                            log.info("Expired auth: {}", authResponse);
                        }
                        return null;
                    }
                    throw new IllegalStateException("Failed to login to fb: " + authResponse, e);
                }
            });
    }
}

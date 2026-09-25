package taninim.fb;

import module java.base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class DefaultFbAuthenticator implements Authenticator {

    private static final Logger log = LoggerFactory.getLogger(DefaultFbAuthenticator.class);

    private final Supplier<char[]> appSecret = FbSec.secretsProvider();

    @Override
    public Optional<ExtUser> authenticate(ExtAuthResponse authResponse) {
        var id = authResponse.userID();
        try {
            log.debug("Looking up {}", authResponse);
            return getExtUser(authResponse, id).filter(remoteUser -> {
                log.debug("Retrieved user {}/{}, {}", remoteUser, remoteUser.id(), authResponse);
                if (remoteUser.hasId(id)) {
                    return true;
                }
                log.debug("Disallowed {}: {}", authResponse, remoteUser);
                return false;
            });
        } catch (Exception e) {
            throw new IllegalStateException("Login failed for user: " + id, e);
        }
    }

    private Optional<ExtUser> getExtUser(ExtAuthResponse authResponse, String id) throws IOException {
        var response = send(uri(authResponse, id));
        if (response.statusCode() == 200) {
            return Optional.of(ExtUserRW.INSTANCE.stringReader().read(response.body()));
        }
        if (response.body().toLowerCase(Locale.ROOT).contains("has expired")) {
            if (log.isDebugEnabled()) {
                log.debug("Expired auth: {}: {}", authResponse, response.body());
            } else {
                log.info("Expired auth: {}", authResponse);
            }
            return Optional.empty();
        }
        throw new IllegalStateException(
            "Failed to login to fb: " + authResponse + ", " + response.statusCode() + ": " + response.body()
        );
    }

    private URI uri(ExtAuthResponse authResponse, String id) {
        var token = authResponse.accessToken();
        return URI.create(GRAPH + "/" + encode(id) +
                          "?fields=id,name" +
                          "&access_token=" + encode(token) +
                          "&appsecret_proof=" + proof(token));
    }

    private String proof(String token) {
        try {
            var mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(new String(appSecret.get()).getBytes(UTF_8), HMAC));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute appsecret_proof", e);
        }
    }

    private static HttpResponse<String> send(URI uri) throws IOException {
        try {
            return HTTP_CLIENT.send(HttpRequest.newBuilder(uri).GET().build(), STRING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    private static final String GRAPH = "https://graph.facebook.com/v26.0";

    private static final String HMAC = "HmacSHA256";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final HttpResponse.BodyHandler<String> STRING = HttpResponse.BodyHandlers.ofString();
}

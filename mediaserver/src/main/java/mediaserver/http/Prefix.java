package mediaserver.http;

import java.util.Optional;

public enum Prefix {

    LOGIN(false),

    RES(false),

    ALBUM(),

    AUDIO(),

    PLAYLIST,

    AUTH(false),

    UNAUTH(false),

    DEBUG(false),

    COOKIESPLEASE(false),

    FAVICON_ICO("favicon.ico", false),

    INDEX("");

    private final String pref;

    private final boolean authenticated;

    private final int length;

    Prefix() {

        this(null);
    }

    Prefix(String pref) {

        this(pref, true);
    }

    Prefix(boolean authenticated) {

        this(null, authenticated);
    }

    Prefix(String pref, boolean authenticated) {

        this.pref = "/" + (pref == null ? name().toLowerCase() : pref);
        this.authenticated = authenticated;
        this.length = this.pref.length();
    }

    public String getPref() {

        return pref;
    }

    public boolean isAuthenticated() {

        return authenticated;
    }

    public static Optional<Prefix> getFor(String uri) {

        for (Prefix prefix : values()) {
            if (prefix.resolves(uri)) {
                return Optional.of(prefix);
            }
        }
        return Optional.empty();
    }

    public String resolve(String uri) {

        if (resolves(uri)) {
            return uri.substring(length);
        }
        throw new IllegalArgumentException(this + ": Invalid uri: " + uri);
    }

    private boolean resolves(String uri) {

        return uri.startsWith(pref);
    }
}

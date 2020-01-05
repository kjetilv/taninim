package mediaserver.http;

import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;

import java.util.Arrays;
import java.util.Optional;

public enum Page {

    DEBUG(AccessLevel.NONE),

    LOGIN(AccessLevel.NONE),

    AUTH(AccessLevel.NONE),

    RES(AccessLevel.NONE),

    FAVICON_ICO("favicon.ico", AccessLevel.NONE),

    COOKIESPLEASE(AccessLevel.LOGIN),

    UNAUTH(AccessLevel.LOGIN),

    ALBUM(AccessLevel.LOGIN),

    AUDIO(AccessLevel.STREAM_CURATED),

    PLAYLIST(AccessLevel.STREAM),

    ADMIN(AccessLevel.ADMIN),

    INDEX("", AccessLevel.LOGIN);

    private final String pref;

    private final AccessLevel accessLevel;

    private final int length;

    Page(AccessLevel accessLevel) {

        this(null, accessLevel);
    }

    Page(String pref, AccessLevel accessLevel) {

        this.pref = "/" + (pref == null ? name().toLowerCase() : pref);
        this.accessLevel = accessLevel;
        this.length = this.pref.length();
    }

    public String getPref() {

        return pref;
    }

    public boolean accessibleIn(Session session) {

        if (session == null) {
            return this.accessLevel == AccessLevel.NONE;
        }
        return this.accessLevel.ordinal() <= accessLevel.ordinal();
    }

    public boolean accessibleWith(AccessLevel accessLevel) {

        return this.accessLevel.ordinal() <= accessLevel.ordinal();
    }

    public static Optional<Page> get(String uri) {

        return Arrays.stream(values()).filter(page -> page.resolves(uri)).findFirst();
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

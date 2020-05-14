package mediaserver.http;

import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;

import java.util.Arrays;
import java.util.stream.Stream;

public enum Route {

    LOGIN(AccessLevel.NONE, Method.GET),

    AUTH(AccessLevel.NONE, Method.POST),

    RES(AccessLevel.NONE, Method.GET),

    FAVICON_ICO("favicon.ico", AccessLevel.NONE, Method.GET),

    COOKIESPLEASE(AccessLevel.LOGIN, Method.POST),

    UNAUTH(AccessLevel.LOGIN, Method.GET, Method.POST),

    ALBUM(AccessLevel.LOGIN, Method.GET),

    AUDIO(AccessLevel.STREAM_CURATED, Method.GET, Method.HEAD),

    PLAYLIST(AccessLevel.STREAM, Method.GET),

    ADMIN(AccessLevel.ADMIN, Method.GET, Method.POST),

    DEBUG(AccessLevel.ADMIN, Method.GET),

    INDEX("", AccessLevel.LOGIN, Method.GET);

    private final String pref;

    private final AccessLevel accessLevel;

    private final Method[] methods;

    private final int length;

    Route(AccessLevel accessLevel, Method... methods) {

        this(null, accessLevel, methods);
    }

    Route(String pref, AccessLevel accessLevel, Method... methods) {

        this.pref = "/" + (pref == null ? name().toLowerCase() : pref);
        this.accessLevel = accessLevel;
        this.methods = methods;
        this.length = this.pref.length();
    }

    public String getPref() {

        return pref;
    }

    public boolean accessibleBy(String method) {

        return Arrays.stream(methods).anyMatch(m -> m.test(method));
    }

    public boolean accessibleIn(Session session) {

        if (session == null) {
            return this.accessLevel == AccessLevel.NONE;
        }
        return this.accessLevel.ordinal() <= accessLevel.ordinal();
    }

    public boolean accessibleWith(AccessLevel accessLevel) {

        return accessLevel.ordinal() >= this.accessLevel.ordinal();
    }

    public static Stream<Route> get(String uri) {

        return Arrays.stream(values()).filter(page -> page.resolves(uri)).limit(1);
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

    enum Method {

        HEAD, GET, POST;

        private boolean test(String s) {

            return name().equalsIgnoreCase(s);
        }
    }
}

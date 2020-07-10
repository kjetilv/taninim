package mediaserver.http;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;

public class Route {

    private final String prefix;

    private final AccessLevel accessLevel;

    private final Method[] methods;

    private final int prefixLength;

    private final String toString;

    public Route(String prefix, AccessLevel accessLevel, Route.Method... methods) {
        Objects.requireNonNull(prefix, "prefix");
        this.prefix = prefix.startsWith("/") ? prefix : "/" + prefix;
        this.prefixLength = this.prefix.length();
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.methods = methods;
        if (methods.length == 0) {
            throw new IllegalArgumentException("No methods for " + this.prefix);
        }
        toString = getClass().getSimpleName() + "[" +
            Arrays.stream(methods).map(Method::name).collect(Collectors.joining(", ")) +
            ":" + prefix + "]";
    }

    public String getPrefix() {
        return prefix;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public boolean accessibleWith(AccessLevel accessLevel) {
        return accessLevel.ordinal() >= this.accessLevel.ordinal();
    }

    public String resolve(String uri) {
        if (resolves(uri)) {
            return uri.substring(prefixLength);
        }
        throw new IllegalArgumentException(this + ": Invalid uri: " + uri);
    }

    public boolean resolves(String uri) {
        return uri.length() >= prefixLength && uri.startsWith(prefix);
    }

    boolean accessibleBy(String method) {
        return Arrays.stream(methods).anyMatch(m -> m.test(method));
    }

    boolean accessibleIn(Session session) {
        return session == null
            ? this.accessLevel == AccessLevel.NONE
            : this.accessLevel.ordinal() <= accessLevel.ordinal();
    }

    public enum Method {
        HEAD, GET, POST;

        private boolean test(String s) {
            return name().equalsIgnoreCase(s);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Route && Objects.equals(prefix, ((Route) o).prefix);
    }

    @Override
    public int hashCode() {
        return prefix.hashCode();
    }

    @Override
    public String toString() {
        return toString;
    }
}

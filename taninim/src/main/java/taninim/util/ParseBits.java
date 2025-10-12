package taninim.util;

import module java.base;

@SuppressWarnings("unused")
public final class ParseBits {

    public static Optional<String> tailString(String line, String prefix) {
        return Optional.ofNullable(line)
            .filter(l -> l.toLowerCase(Locale.ROOT).startsWith(prefix))
            .flatMap(l -> tailString(l, prefix.length()));
    }

    public static Optional<String> tailString(String line, int index) {
        var lastIndex = line.length() - 1;
        return index < lastIndex
            ? Optional.of(line.substring(index))
            : Optional.empty();
    }

    public static Optional<String> headString(String s, int index) {
        var lastIndex = s.length() - 1;
        return index < lastIndex
            ? Optional.of(s.substring(0, index))
            : Optional.empty();
    }

    private ParseBits() {
    }
}

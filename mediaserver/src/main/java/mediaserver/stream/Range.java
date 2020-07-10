package mediaserver.stream;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static java.lang.Long.parseLong;

final class Range {

    private final Long start;

    private final Long exclusiveEnd;

    private final boolean satisfiable;

    private Range(Long start, Long exclusiveEnd, long length) {
        this.start = start;
        this.exclusiveEnd = exclusiveEnd;
        this.satisfiable = start == null || start < length;
    }

    static Stream<Range> read(String value, long length) {
        return Optional.ofNullable(value).stream()
            .map(COMMA::split)
            .flatMap(Arrays::stream)
            .map(String::trim)
            .map(header -> {
                if (header.startsWith(BYTES_PREAMBLE)) {
                    String range = header.substring(BYTES_PREAMBLE_LENGTH).trim();
                    int dashIndex = range.indexOf('-');
                    if (range.lastIndexOf('-') != dashIndex) {
                        throw new IllegalStateException("Invalid byte range, length " + length + ": " + value);
                    }
                    Long start = dashIndex == 0 ? null : parseLong(range.substring(0, dashIndex));
                    Long exclusiveEnd = range.endsWith("-") ? null : parseLong(range.substring(dashIndex + 1)) + 1;
                    return new Range(start, exclusiveEnd, length);
                }
                throw new IllegalArgumentException("Not a valid range header: " + header);
            });
    }

    long getStart() {
        return start;
    }

    boolean isSatisfiable() {
        return satisfiable;
    }

    long getExclusiveEnd(long max) {
        return exclusiveEnd == null ? max : Math.min(exclusiveEnd, max);
    }

    private static final String BYTES_PREAMBLE = BYTES + "=";

    private static final int BYTES_PREAMBLE_LENGTH = BYTES_PREAMBLE.length();

    private static final Pattern COMMA = Pattern.compile(",");

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + (start == null ? "" : start) + '-' + (exclusiveEnd == null ? "" : exclusiveEnd) + "]";
    }
}

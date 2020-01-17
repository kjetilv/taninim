package mediaserver.toolkit;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static java.lang.Long.parseLong;

public final class BytesRange {

    public static final String BYTES_PREAMBLE = BYTES + "=";

    private static final int BYTES_PREAMBLE_LENGTH = BYTES_PREAMBLE.length();

    private final Long start;

    private final Long exclusiveEnd;

    private final boolean satisfiable;

    private BytesRange(Long start, Long exclusiveEnd, long length) {

        this.start = start;
        this.exclusiveEnd = exclusiveEnd;
        this.satisfiable = start == null || start < length;
    }

    public static Stream<BytesRange> read(String value, long length) {

        return Optional.ofNullable(value)
            .stream()
            .map(header ->
                header.split(","))
            .flatMap(Arrays::stream)
            .map(String::trim)
            .map(header -> {
                if (header.startsWith(BYTES_PREAMBLE)) {
                    String range = header.substring(BYTES_PREAMBLE_LENGTH).trim();
                    int split = range.indexOf('-');
                    if (range.lastIndexOf('-') != split) {
                        throw new IllegalStateException("Invalid byte range, length " + length + ": " + value);
                    }
                    return new BytesRange(
                        split == 0 ? null : parseLong(range.substring(0, split)),
                        range.endsWith("-") ? null : parseLong(range.substring(split + 1)) + 1,
                        length);
                }
                throw new IllegalArgumentException("Not a valid range header: " + header);
            });
    }

    public boolean isSatisfiable() {

        return satisfiable;
    }

    public long getStart() {

        return start;
    }

    public long getExclusiveEnd(long max) {

        return exclusiveEnd == null ? max : Math.min(exclusiveEnd, max);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[" + BYTES_PREAMBLE + (start == null ? "" : start) + '-' + (exclusiveEnd == null ? "" : exclusiveEnd) + "]";
    }
}

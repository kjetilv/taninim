package mediaserver.gui;

import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;

public class BytesRange {

    public static final String BYTES_PREAMBLE = BYTES + "=";

    private static final int BYTES_PREAMBLE_LENGTH = BYTES_PREAMBLE.length();

    private final Long start;

    private final Long end;

    public BytesRange(Long start, Long end) {

        this.start = start;
        this.end = end;
    }

    public static Optional<BytesRange> read(String value) {

        return Optional.ofNullable(value).map(header -> {
            if (header.startsWith(BYTES_PREAMBLE)) {
                String range = header.substring(BYTES_PREAMBLE_LENGTH).trim();
                int split = range.indexOf('-');
                return new BytesRange(
                    split == 0
                        ? null
                        : Long.parseLong(range.substring(0, split)),
                    range.endsWith("-")
                        ? null
                        : Long.parseLong(range.substring(split + 1)) + 1);
            }
            throw new IllegalArgumentException("Not a valid range header: " + header);
        });
    }

    public boolean isSatisfiable(long length) {

        return start == null || start < length;
    }

    public long getStart() {

        return start;
    }

    public long getEndExclusive(long length) {

        return end == null ? length : Math.min(end, length);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[" + BYTES_PREAMBLE + (start == null ? "" : start) + '-' + (end == null ? "" : end) + "]";
    }
}

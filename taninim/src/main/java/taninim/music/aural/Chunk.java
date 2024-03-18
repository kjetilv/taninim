package taninim.music.aural;

import com.github.kjetilv.uplift.kernel.io.Range;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.stream.LongStream;

@SuppressWarnings("unused")
public record Chunk(
    String format,
    long start,
    long end,
    long totalSize
) {

    public static Optional<Chunk> create(Range range, String format, long transferSize) {
        if (range.length() == null) {
            throw new IllegalStateException("Cannot compute chunk, missing length: " + range);
        }
        Long start = range.start();
        if (start == null || start < range.length()) {
            try {
                return Optional.of(new Chunk(
                    format,
                    start == null ? 0 : start,
                    computeExclusiveEnd(range, range.length(), transferSize),
                    range.length()
                ));
            } catch (Exception e) {
                throw new IllegalStateException(
                    range + " could not create chunk for format " + format + ", transferSize " + transferSize, e);
            }
        }
        return Optional.empty();
    }

    public Chunk(String format, long totalSize) {
        this(format, 0, 0, totalSize);
    }

    public Chunk tail(long split) {
        return new Chunk(format, split, end, totalSize);
    }

    public Chunk head(long split) {
        return new Chunk(format, start, split, totalSize);
    }

    public String rangeResponseHeader() {
        return BYTES + rangeResponse();
    }

    public Range range() {
        return new Range(start, end);
    }

    public double getPerc(long progress, int decs) {
        int dims = Math.toIntExact(Math.round(StrictMath.pow(10, decs)));
        return Math.toIntExact(100 * dims * (start + progress) / totalSize) / (double) dims;
    }

    public long length() {
        return end - start;
    }

    private String rangeRequest() {
        return start + "-" + (end - 1);
    }

    private String rangeResponse() {
        return rangeRequest() + "/" + totalSize;
    }

    private static final String BYTES = "bytes ";

    private static long computeExclusiveEnd(Range range, long fileSize, long transferSize) {
        long specifiedEnd = range.exclusiveEnd() == null ? fileSize : Math.min(range.exclusiveEnd(), fileSize);
        boolean truncate = transferSize > 0 && transferSize < fileSize;
        return LongStream.of(
                specifiedEnd,
                range.start() + fileSize,
                truncate
                    ? range.start() + transferSize
                    : Long.MAX_VALUE
            )
            .min()
            .orElseThrow();
    }

    @Override
    public String toString() {
        long startPerc = start * 100 / totalSize;
        long endPerc = end * 100 / totalSize;
        return MessageFormat.format(
            "{0}[{1} {2}%]",
            getClass().getSimpleName(),
            rangeResponseHeader(),
            startPerc == endPerc ? startPerc : startPerc + "-" + endPerc
        );
    }
}

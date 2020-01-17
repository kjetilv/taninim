package mediaserver.toolkit;

import io.netty.handler.codec.http.HttpHeaderValues;

public final class Chunk {

    private final long start;

    private final long end;

    private final long totalSize;

    public Chunk(long totalSize) {
        this(0, 0, totalSize);
    }

    public Chunk(long start, long end, long totalSize) {

        this.start = start;
        this.end = end;
        this.totalSize = totalSize;
    }

    public String range() {

        return HttpHeaderValues.BYTES + " " + start + "-" + (end - 1) + "/" + totalSize;
    }

    public long getEnd() {

        return end;
    }

    public long getStart() {

        return start;
    }

    public long getSize() {

        return end - start;
    }

    public long getTotalSize() {

        return totalSize;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + range() + " [" + getSize() + "]]";
    }
}

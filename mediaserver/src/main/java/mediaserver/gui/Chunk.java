package mediaserver.gui;

import io.netty.handler.codec.http.HttpHeaderValues;

final class Chunk {

    private final long start;

    private final long end;

    private final long totalSize;

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

    long getStart() {

        return start;
    }

    long getSize() {

        return end - start;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + range() + " [" + getSize() + "]]";
    }
}

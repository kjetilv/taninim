package mediaserver.stream;

public final class Chunk {

    private final long start;

    private final long end;

    private final long totalSize;

    private static final String BYTES = "bytes ";

    Chunk(long totalSize) {

        this(0, 0, totalSize);
    }

    Chunk(long start, long end, long totalSize) {

        this.start = start;
        this.end = end;
        this.totalSize = totalSize;
    }

    String getRangeHeader() {

        return BYTES + getRange();
    }

    String getRange() {

        return start + "-" + (end - 1) + "/" + totalSize;
    }

    long getEnd() {

        return end;
    }

    long getStart() {

        return start;
    }

    long getSize() {

        return end - start;
    }

    long getTotalSize() {

        return totalSize;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + getRange() + " [" + getSize() + "]]";
    }
}

package mediaserver.stream;

public final class Chunk
{

    private final long start;

    private final long end;

    private final long totalSize;

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

    int perMille(long progress) {
        return Math.toIntExact(1000 * (start + progress) / totalSize);
    }

    private static final String BYTES = "bytes ";

    @Override
    public String toString() {

        int startPerc = (int) (start * 100 / totalSize);
        int endPerc = (int) (end * 100 / totalSize);
        return getClass().getSimpleName() + "[" + getRange() + " " + (int) (getSize() / 1024) + "Kb/" +
            (startPerc == endPerc ? startPerc : startPerc + "-" + endPerc) + "%]";
    }
}

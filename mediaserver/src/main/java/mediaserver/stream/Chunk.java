package mediaserver.stream;

public final class Chunk {

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

    @Override
    public String toString() {
        long startPerc = start * 100 / totalSize;
        long endPerc = end * 100 / totalSize;
        return getClass().getSimpleName() + "[" + getRange() + " " + getSize() / KILO + "Kb/" +
            (startPerc == endPerc ? startPerc : startPerc + "-" + endPerc) + "%]";
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

    long getTotalSize() {
        return totalSize;
    }

    double getPerc(long progress, int decs) {
        int dims = StrictMath.toIntExact(
            StrictMath.round(
                StrictMath.pow(10, decs)));
        return Math.toIntExact(100 * dims * (start + progress) / totalSize) / (double) dims;
    }

    long getSize() {
        return end - start;
    }

    private static final String BYTES = "bytes ";

    private static final int KILO = 1024;
}

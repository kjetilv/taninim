package mediaserver.gui;

final class Chunk {

    private final long startOffset;

    private final long endOffset;

    private final long size;

    private final long totalSize;

    public Chunk(long startOffset, long endOffset, long size, long totalSize) {

        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.size = size;
        this.totalSize = totalSize;
    }

    long getStartOffset() {

        return startOffset;
    }

    public long getEndOffset() {

        return endOffset;
    }

    long getSize() {

        return size;
    }

    long getTotalSize() {

        return totalSize;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            startOffset + "-" + endOffset +
            " " + size + "/" + totalSize +
            "]";
    }
}

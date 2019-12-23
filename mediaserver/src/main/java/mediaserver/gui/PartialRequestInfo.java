package mediaserver.gui;

final class PartialRequestInfo {

    private final long startOffset;

    private final long endOffset;

    private final long chunkSize;

    public PartialRequestInfo(long startOffset, long endOffset, long chunkSize) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.chunkSize = chunkSize;
    }

    public long getEndOffset() {
        return endOffset;
    }

    long getStartOffset() {
        return startOffset;
    }

    long getChunkSize() {
        return chunkSize;
    }
}

package mediaserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public final class RingBuffer<T> {

    private final LinkedList<T> buffer;

    private final int size;

    public RingBuffer(int size) {
        this.size = size;
        this.buffer = new LinkedList<>();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + size + "]";
    }

    public void add(T t) {
        synchronized (buffer) {
            buffer.addLast(t);
            if (buffer.size() > size) {
                buffer.removeFirst();
            }
        }
    }

    public Collection<T> get() {
        synchronized (buffer) {
            return new ArrayList<>(buffer);
        }
    }
}

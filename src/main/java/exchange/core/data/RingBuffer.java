package exchange.core.data;

import java.util.function.Supplier;

public final class RingBuffer<T> {

    private final T[] buffer;
    private final int size;
    private final int mask;

    // monotonic sequences
    private final Sequence producerSeq = new Sequence();
    private final Sequence consumerSeq = new Sequence();

    @SuppressWarnings("unchecked")
    public RingBuffer(int size, Supplier<T> factory) {
        if (Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("size must be power of 2");
        }

        this.size = size;
        this.mask = size - 1;
        this.buffer = (T[]) new Object[size];

        // pre-allocate (NO GC at runtime)
        for (int i = 0; i < size; i++) {
            buffer[i] = factory.get();
        }
    }

    /* ================= PRODUCER ================= */

    /**
     * Try claim next slot (NON-BLOCKING)
     * @return sequence or -1 if buffer is full
     */
    public long tryNext() {
        final long next = producerSeq.get() + 1;

        // full when producer laps consumer
        if (next - size > consumerSeq.get()) {
            return -1;
        }

        // publish sequence (release)
        producerSeq.set(next);
        return next;
    }

    public T get(long seq) {
        return buffer[(int) (seq & mask)];
    }

    public void publish(long seq) {
        // no-op: producerSeq.set() already published
    }

    /* ================= CONSUMER ================= */

    /**
     * Poll next available event or null if empty
     */
    public T poll() {
        final long next = consumerSeq.get() + 1;

        if (next > producerSeq.get()) {
            return null;
        }

        consumerSeq.set(next);
        return buffer[(int) (next & mask)];
    }

    /* ================= UTIL ================= */

    public long size() {
        return producerSeq.get() - consumerSeq.get();
    }

    public boolean isFull() {
        return size() >= size;
    }

    public boolean isReady() {
        return size() <= size*0.8;
    }


    public Sequence consumerSequence() {
        return consumerSeq;
    }
}

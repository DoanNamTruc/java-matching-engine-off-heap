package exchange.core.data;

public final class Sequence {

    private volatile long value = -1;

    public long get() {
        return value;
    }

    public void set(long v) {
        value = v;
    }

    public long incrementAndGet() {
        return ++value;
    }
}

package exchange.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import exchange.backup.Snapshotting;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
public final class OffHeapOrderBook implements Snapshotting {

    private static final int SNAPSHOT_MAGIC = 0xCAFEBABE;
    private static final int SNAPSHOT_VERSION = 1;

    private static final int ORDER_SIZE = 28;

    private static final int EXPECTED_PRICE_LEVELS = 65_536;

    private final ByteBuffer mem;
    private int freePtr = 0;
    private int freeListHead = -1;

    private long lastProcessedSequence;

    // price -> head order ptr
    private final Long2IntOpenHashMap bids = new Long2IntOpenHashMap(EXPECTED_PRICE_LEVELS);
    private final Long2IntOpenHashMap asks = new Long2IntOpenHashMap(EXPECTED_PRICE_LEVELS);

    private long bestBidPrice = Long.MIN_VALUE;
    private long bestAskPrice = Long.MAX_VALUE;

    public OffHeapOrderBook(int maxOrders) {
        mem = ByteBuffer.allocateDirect(maxOrders * ORDER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        bids.defaultReturnValue(-1);
        asks.defaultReturnValue(-1);
    }

    /* ===== ALLOC ===== */

    public int alloc(long id, long price, long qty) {
        final int ptr;
        if (freeListHead != -1) {
            // reuse
            ptr = freeListHead;
            freeListHead = mem.getInt(ptr + 24);
        } else {
            // new allocation
            ptr = freePtr;
            freePtr += ORDER_SIZE;

            if (freePtr > mem.capacity()) {
                throw new OutOfMemoryError("Off-heap exhausted");
            }
        }

        mem.putLong(ptr, id);
        mem.putLong(ptr + 8, price);
        mem.putLong(ptr + 16, qty);
        mem.putInt(ptr + 24, -1);
        return ptr;
    }

    public void free(int ptr) {
        // link into free list
        mem.putInt(ptr + 24, freeListHead);
        freeListHead = ptr;
    }

    /* ===== ACCESS ===== */
    public long id(int p) { return mem.getLong(p); }
    public long price(int p) { return mem.getLong(p + 8); }
    public long qty(int p) { return mem.getLong(p + 16); }
    public void setQty(int p, long q) { mem.putLong(p + 16, q); }
    public int next(int p) { return mem.getInt(p + 24); }
    public void setNext(int p, int n) { mem.putInt(p + 24, n); }

    /* ===== PRICE LEVEL ===== */

    public void addBid(int ptr) {
        long p = price(ptr);
        int head = bids.get(p);
        setNext(ptr, head);
        bids.put(p, ptr);
        if (p > bestBidPrice) bestBidPrice = p;
    }

    public void addAsk(int ptr) {
        long p = price(ptr);
        int head = asks.get(p);
        setNext(ptr, head);
        asks.put(p, ptr);
        if (p < bestAskPrice) bestAskPrice = p;
    }

    public int bestBidHead() {
        return bids.get(bestBidPrice);
    }

    public int bestAskHead() {
        return asks.get(bestAskPrice);
    }

    public long bestBidPrice() { return bestBidPrice; }
    public long bestAskPrice() { return bestAskPrice; }

    public void removeEmptyBidLevel() {
        bids.remove(bestBidPrice);
        bestBidPrice = bids.isEmpty()
                ? Long.MIN_VALUE
                : bids.keySet().longStream().max().getAsLong();
    }

    public void removeEmptyAskLevel() {
        asks.remove(bestAskPrice);
        bestAskPrice = asks.isEmpty()
                ? Long.MAX_VALUE
                : asks.keySet().longStream().min().getAsLong();
    }

    @Override
    public void snapshot(DataOutputStream out) throws IOException {

        // ===== HEADER =====
        out.writeInt(SNAPSHOT_MAGIC);
        out.writeInt(SNAPSHOT_VERSION);

        out.writeLong(bestBidPrice);
        out.writeLong(bestAskPrice);
        out.writeInt(freePtr);

        // ===== BIDS =====
        out.writeInt(bids.size());
        for (var e : bids.long2IntEntrySet()) {
            out.writeLong(e.getLongKey()); // price
            out.writeInt(e.getIntValue()); // head ptr
        }

        // ===== ASKS =====
        out.writeInt(asks.size());
        for (var e : asks.long2IntEntrySet()) {
            out.writeLong(e.getLongKey());
            out.writeInt(e.getIntValue());
        }

        // ===== OFF-HEAP MEMORY =====
        int usedBytes = freePtr;
        out.writeInt(usedBytes);

        ByteBuffer dup = mem.duplicate();
        dup.clear();
        dup.limit(usedBytes);

        byte[] tmp = new byte[8192];
        while (dup.hasRemaining()) {
            int len = Math.min(tmp.length, dup.remaining());
            dup.get(tmp, 0, len);
            out.write(tmp, 0, len);
        }
    }



    @Override
    public void restore(DataInputStream in) throws IOException {

        // ===== HEADER =====
        int magic = in.readInt();
        if (magic != SNAPSHOT_MAGIC) {
            throw new IOException("Invalid snapshot magic");
        }

        int version = in.readInt();
        if (version != SNAPSHOT_VERSION) {
            throw new IOException("Unsupported snapshot version");
        }

        bestBidPrice = in.readLong();
        bestAskPrice = in.readLong();
        freePtr = in.readInt();

        // ===== BIDS =====
        bids.clear();
        int bidSize = in.readInt();
        for (int i = 0; i < bidSize; i++) {
            long price = in.readLong();
            int ptr = in.readInt();
            bids.put(price, ptr);
        }

        // ===== ASKS =====
        asks.clear();
        int askSize = in.readInt();
        for (int i = 0; i < askSize; i++) {
            long price = in.readLong();
            int ptr = in.readInt();
            asks.put(price, ptr);
        }

        // ===== OFF-HEAP MEMORY =====
        int usedBytes = in.readInt();
        if (usedBytes > mem.capacity()) {
            throw new IOException("Snapshot memory larger than buffer");
        }

        ByteBuffer dup = mem.duplicate();
        dup.clear();
        dup.limit(usedBytes);

        byte[] tmp = new byte[8192];
        int remaining = usedBytes;
        while (remaining > 0) {
            int len = Math.min(tmp.length, remaining);
            in.readFully(tmp, 0, len);
            dup.put(tmp, 0, len);
            remaining -= len;
        }
    }

    public long getLastProcessedSequence() {
        return lastProcessedSequence;
    }

}

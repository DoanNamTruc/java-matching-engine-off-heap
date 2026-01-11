package exchange;

import exchange.core.*;
import exchange.core.data.EventLog;
import exchange.core.data.OrderEvent;
import exchange.core.data.ResultEvent;
import exchange.net.*;
import exchange.core.data.RingBuffer;
import exchange.backup.SnapshotService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {

        final int MAX_ORDERS = 10000000;

        // ===== OrderBook =====
        OffHeapOrderBook orderBook = new OffHeapOrderBook(MAX_ORDERS);

        // ===== RingBuffer =====
        RingBuffer<OrderEvent> ring =
                new RingBuffer<>(1024*1024, OrderEvent::new);

        RingBuffer<ResultEvent> outRing =
                new RingBuffer<>(1024, ResultEvent::new);

        // ===== Snapshot =====
        Path snapshotPath = Paths.get("orderbook.snapshot");

        long snapshotSeq = 0;
        if (Files.exists(snapshotPath)) {
            SnapshotService.load(snapshotPath, orderBook);
            snapshotSeq = orderBook.getLastProcessedSequence(); // 👈 bạn cần field này
            System.out.println("Loaded snapshot @ seq=" + snapshotSeq);
        }

        // ===== Set consumer sequence =====
        ring.consumerSequence().set(snapshotSeq);

        // ===== Event log =====
        EventLog eventLog = new EventLog(Paths.get("events.log"));

        // ===== Replay log BEFORE starting matching thread =====
        eventLog.replayFrom(snapshotSeq + 1, event -> {
            long seq = ring.tryNext();
            OrderEvent e = ring.get(seq);
            e.from(event);
            ring.publish(seq);
        });

        // ===== Start matching engine =====
        Thread matching =
                new Thread(new MatchingEngine(ring, outRing, orderBook), "matching-thread");
        matching.start();
        // ===== Start Netty =====
        ResultSender sender = new ResultSender(outRing);
        new Thread(sender, "result-sender").start();

        // ===== Start Netty =====
        new NettyServer(ring, sender).start(9000);
    }
}

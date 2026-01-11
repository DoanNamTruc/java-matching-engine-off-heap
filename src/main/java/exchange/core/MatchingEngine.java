package exchange.core;

import exchange.core.data.OrderEvent;
import exchange.core.data.ResultEvent;
import exchange.core.data.RingBuffer;
import exchange.backup.SnapshotService;
import exchange.idle.model.SleepWaitStrategy;
import exchange.idle.WaitStrategy;
import io.netty.channel.Channel;

import java.io.IOException;
import java.nio.file.Paths;

public final class MatchingEngine implements Runnable {

    private static final int BATCH_SIZE     = 1000;

    private final RingBuffer<OrderEvent> ring;
    private final RingBuffer<ResultEvent> outRing;

    private final OffHeapOrderBook book;
    private final WaitStrategy waitStrategy = new SleepWaitStrategy();

    public MatchingEngine(RingBuffer<OrderEvent> ring, RingBuffer<ResultEvent> outRing, OffHeapOrderBook orderBook) {
        this.ring = ring;
        this.outRing = outRing;
        this.book = orderBook;
    }

    @Override
    public void run() {
        long processedTotal = 0;
        long timestop = System.nanoTime();

        int batch = 0;
        Channel lastChannel = null;

        while (true) {
            OrderEvent e = ring.poll();

            if (e == null) {
                if (batch > 0) {
                    maybeResumeRead(lastChannel);
                    batch = 0;
                    lastChannel = null;
                }
                waitStrategy.idle();
                continue;
            }

            match(e);
            e.reset();

            processedTotal++;
            batch++;
            lastChannel = e.channel;

            if (batch >= BATCH_SIZE) {
                maybeResumeRead(lastChannel);
                batch = 0;
                lastChannel = null;
            }

            if (processedTotal % 100_000 == 0) {
                System.out.println(
                        "Processed " + processedTotal +
                                " in " + (System.nanoTime() - timestop) / 1_000_000 + " ms"
                );
                timestop = System.nanoTime();

                try {
                    SnapshotService.save(
                            Paths.get("orderbook.snapshot"), book
                    );
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private void match(OrderEvent e) {
        if (e.side == 0) {
            matchBuy(e);
        } else {
            matchSell(e);
        }
    }

    private void matchBuy(OrderEvent e) {
        long qty = e.qty;

        while (qty > 0 && book.bestAskPrice() <= e.price) {
            int ask = book.bestAskHead();
            long traded = Math.min(qty, book.qty(ask));

            qty -= traded;
            book.setQty(ask, book.qty(ask) - traded);
            if (book.qty(ask) == 0) {
                book.removeEmptyAskLevel();
            }
            publishTrade(e.orderId, book.id(ask), ask, traded);
        }

        if (qty > 0) {
            int ptr = book.alloc(e.orderId, e.price, qty);
            book.addBid(ptr);
        }
    }

    private void matchSell(OrderEvent e) {
        long qty = e.qty;

        while (qty > 0 && book.bestBidPrice() >= e.price) {
            int bid = book.bestBidHead();
            long traded = Math.min(qty, book.qty(bid));

            qty -= traded;
            book.setQty(bid, book.qty(bid) - traded);
            if (book.qty(bid) == 0) {
                book.removeEmptyBidLevel();
            }
            publishTrade(book.id(bid), e.orderId, bid, traded);
        }

        if (qty > 0) {
            int ptr = book.alloc(e.orderId, e.price, qty);
            book.addAsk(ptr);
        }
    }

    private void maybeResumeRead(Channel ch) {
        if (ch == null) return;

        // ⚠ MUST run on EventLoop thread
        if (!ring.isFull()) {
            ch.eventLoop().execute(() ->
                    ch.config().setAutoRead(true)
            );
        }
    }

    private void publishTrade(long buyId, long sellId,
                              long price, long qty) {
                long seq = outRing.tryNext();
                ResultEvent e = outRing.get(seq);
                e.buyOrderId = buyId;
                e.sellOrderId = sellId;
                e.price = price;
                e.qty = qty;
                outRing.consumerSequence().set(seq);
    }
}

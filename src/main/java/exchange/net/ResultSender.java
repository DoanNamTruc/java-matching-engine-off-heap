package exchange.net;

import exchange.core.data.ResultEvent;
import exchange.core.data.RingBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ResultSender implements Runnable {

    private final RingBuffer<ResultEvent> ring;
    private final Set<Channel> clients = new CopyOnWriteArraySet<>();

    public ResultSender(RingBuffer<ResultEvent> ring) {
        this.ring = ring;
    }

    public void register(Channel ch) {
        clients.add(ch);
    }

    public void unregister(Channel ch) {
        clients.remove(ch);
    }

    @Override
    public void run() {
        while (true) {
            ResultEvent e = ring.poll();
            if (e == null) {
                Thread.onSpinWait();
                continue;
            }

            if (!clients.isEmpty()) {
                for (Channel ch : clients) {
                    if (ch.isActive()) {
                        ch.write(encode(ch, e));
                    }
                }
                // flush theo batch (rất quan trọng)
                for (Channel ch : clients) {
                    ch.flush();
                }
            }

            e.clear();
        }
    }

    private ByteBuf encode(Channel ch, ResultEvent e) {
        ByteBuf buf = ch.alloc().buffer(64);

        buf.writeByte(e.type);

        if (e.type == ResultEvent.TRADE) {
            buf.writeLong(e.buyOrderId);
            buf.writeLong(e.sellOrderId);
            buf.writeLong(e.price);
            buf.writeLong(e.qty);
        } else {
            buf.writeLong(e.orderId);
            buf.writeLong(e.remainingQty);
        }
        return buf;
    }
}

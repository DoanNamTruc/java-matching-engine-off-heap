package exchange.net;

import exchange.core.data.OrderEvent;
import exchange.core.data.RingBuffer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class OrderHandler
        extends SimpleChannelInboundHandler<OrderEvent> {

    private final RingBuffer<OrderEvent> ring;

    public OrderHandler(RingBuffer<OrderEvent> ring) {
        this.ring = ring;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OrderEvent msg) {

        long seq = ring.tryNext();
        if (seq < 0) {
            // 🚨 RING FULL → STOP READING TCP
//            ctx.channel().config().setAutoRead(false);
            return;
        }

        OrderEvent slot = ring.get(seq);
        slot.orderId = msg.orderId;
        slot.symbol  = msg.symbol;
        slot.price   = msg.price;
        slot.qty     = msg.qty;
        slot.side    = msg.side;
        slot.channel = ctx.channel(); // 👈 bind channel

        ring.publish(seq);
    }
}

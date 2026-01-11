package exchange.core.data;

import io.netty.channel.Channel;

public final class OrderEvent {
    public long orderId;
    public int symbol;
    public long price;
    public long qty;
    public byte side; // 0=BUY, 1=SELL

    // 👇 VERY IMPORTANT
    public Channel channel;

    public void from(OrderEvent src) {
        this.orderId = src.orderId;
        this.price   = src.price;
        this.qty     = src.qty;
        this.side    = src.side;
    }

    /* ===== lifecycle ===== */

    public void reset() {
        orderId = 0L;
        symbol  = 0;
        price   = 0L;
        qty     = 0L;
        side    = 0;
        channel = null;
    }
}

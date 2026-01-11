package exchange.net;

import exchange.core.data.OrderEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class OrderDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(
            ChannelHandlerContext ctx,
            ByteBuf in,
            List<Object> out) {

        if (in.readableBytes() < 29) return;

        OrderEvent e = new OrderEvent();
        e.orderId = in.readLong();
        e.symbol = in.readInt();
        e.price = in.readLong();
        e.qty = in.readLong();
        e.side = in.readByte();

        out.add(e);
    }
}

package exchange.core.data;

public final class ResultEvent {

    public static final byte TRADE = 1;
    public static final byte ORDER_UPDATE = 2;

    public byte type;

    // trade
    public long buyOrderId;
    public long sellOrderId;
    public long price;
    public long qty;

    // order update
    public long orderId;
    public long remainingQty;

    public void clear() {
        type = 0;
        buyOrderId = sellOrderId = price = qty = 0;
        orderId = remainingQty = 0;
    }
}

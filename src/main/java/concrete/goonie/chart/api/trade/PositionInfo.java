package concrete.goonie.chart.api.trade;

/**
 * Lightweight trading position model for chart overlays.
 */
public final class PositionInfo {
    public enum Side { BUY, SELL }

    private final String id;
    private final Side side;
    private final double openPrice;
    private final double stopLoss;
    private final double takeProfit;
    private final double quantity;
    private final double pipSize;

    public PositionInfo(String id, Side side, double openPrice, double stopLoss, double takeProfit, double quantity, double pipSize) {
        this.id = id;
        this.side = side == null ? Side.BUY : side;
        this.openPrice = openPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.quantity = quantity;
        this.pipSize = pipSize <= 0.0 ? 0.0001 : pipSize;
    }

    public String getId() { return id; }
    public Side getSide() { return side; }
    public double getOpenPrice() { return openPrice; }
    public double getStopLoss() { return stopLoss; }
    public double getTakeProfit() { return takeProfit; }
    public double getQuantity() { return quantity; }
    public double getPipSize() { return pipSize; }

    public double pipDifferenceTo(double price) {
        return (price - openPrice) / pipSize * (side == Side.BUY ? 1.0 : -1.0);
    }
}

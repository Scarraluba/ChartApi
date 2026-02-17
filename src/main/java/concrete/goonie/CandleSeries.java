package concrete.goonie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CandleSeries {
    private final List<Candle> candles = new ArrayList<>();

    public int size() { return candles.size(); }
    public Candle get(int i) { return candles.get(i); }
    public void add(Candle c) { candles.add(c); }

    public double minLow(int from, int toInclusive) {
        double v = Double.POSITIVE_INFINITY;
        for (int i = from; i <= toInclusive; i++) v = Math.min(v, candles.get(i).low);
        return v;
    }

    public double maxHigh(int from, int toInclusive) {
        double v = Double.NEGATIVE_INFINITY;
        for (int i = from; i <= toInclusive; i++) v = Math.max(v, candles.get(i).high);
        return v;
    }

    // demo generator (optional, still pure Java)
    public static CandleSeries generateDemo(int count) {
        CandleSeries s = new CandleSeries();
        Random r = new Random(7);

        double price = 100.0;
        long t = 0;

        for (int i = 0; i < count; i++) {
            double drift = (r.nextDouble() - 0.5) * 0.6;
            double wave = Math.sin(i * 0.015) * 0.4 + Math.sin(i * 0.045) * 0.25;
            double noise = (r.nextDouble() - 0.5) * 0.8;

            double delta = drift + wave + noise;
            double open = price;
            double close = price + delta;

            double high = Math.max(open, close) + (0.2 + r.nextDouble() * 0.8);
            double low  = Math.min(open, close) - (0.2 + r.nextDouble() * 0.8);

            price = close;
            s.add(new Candle(t++, open, high, low, close));
        }
        return s;
    }
}
package concrete.goonie.chart.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Mutable series of candles for one symbol and timeframe.
 */
public final class CandleSeries {
    private final List<Candle> candles = new ArrayList<>();

    public int size() {
        return candles.size();
    }

    public boolean isEmpty() {
        return candles.isEmpty();
    }

    public Candle get(int index) {
        return candles.get(index);
    }

    public Candle last() {
        return candles.isEmpty() ? null : candles.get(candles.size() - 1);
    }

    public void add(Candle candle) {
        candles.add(Objects.requireNonNull(candle, "candle"));
    }

    public void addAll(List<Candle> values) {
        Objects.requireNonNull(values, "values");
        for (Candle candle : values) {
            add(candle);
        }
    }

    public List<Candle> asList() {
        return Collections.unmodifiableList(candles);
    }

    public double minLow(int from, int toInclusive) {
        validateRange(from, toInclusive);
        double value = Double.POSITIVE_INFINITY;
        for (int i = from; i <= toInclusive; i++) {
            value = Math.min(value, candles.get(i).low());
        }
        return value;
    }

    public double maxHigh(int from, int toInclusive) {
        validateRange(from, toInclusive);
        double value = Double.NEGATIVE_INFINITY;
        for (int i = from; i <= toInclusive; i++) {
            value = Math.max(value, candles.get(i).high());
        }
        return value;
    }

    public LocalDateTime[] copyTime() {
        LocalDateTime[] values = new LocalDateTime[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).time();
        return values;
    }

    public double[] copyOpen() {
        double[] values = new double[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).open();
        return values;
    }

    public double[] copyHigh() {
        double[] values = new double[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).high();
        return values;
    }

    public double[] copyLow() {
        double[] values = new double[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).low();
        return values;
    }

    public double[] copyClose() {
        double[] values = new double[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).close();
        return values;
    }

    public long[] copyVolume() {
        long[] values = new long[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).volume();
        return values;
    }

    public long[] copySpread() {
        long[] values = new long[candles.size()];
        for (int i = 0; i < candles.size(); i++) values[i] = candles.get(i).spread();
        return values;
    }

    public int indexAtOrBefore(LocalDateTime time) {
        if (candles.isEmpty()) return -1;
        int lo = 0;
        int hi = candles.size() - 1;
        int answer = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            LocalDateTime candidate = candles.get(mid).time();
            if (!candidate.isAfter(time)) {
                answer = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return answer;
    }


    public CandleSeries copyRange(int fromInclusive, int toInclusive) {
        CandleSeries copy = new CandleSeries();
        if (candles.isEmpty()) return copy;
        int from = Math.max(0, fromInclusive);
        int to = Math.min(candles.size() - 1, toInclusive);
        for (int i = from; i <= to; i++) {
            copy.add(candles.get(i));
        }
        return copy;
    }

    public CandleSeries copyUntil(LocalDateTime timeInclusive) {
        CandleSeries copy = new CandleSeries();
        for (Candle candle : candles) {
            if (candle.time().isAfter(timeInclusive)) {
                break;
            }
            copy.add(candle);
        }
        return copy;
    }

    public static CandleSeries generateDemo(LocalDateTime start, Timeframe timeframe, int count) {
        Random random = new Random(7L + timeframe.ordinal());
        CandleSeries series = new CandleSeries();
        double price = 100.0 + timeframe.ordinal() * 8.0;
        LocalDateTime time = start;
        for (int i = 0; i < count; i++) {
            double drift = (random.nextDouble() - 0.5) * 0.55;
            double wave = Math.sin(i * 0.018) * 0.45 + Math.sin(i * 0.05) * 0.18;
            double noise = (random.nextDouble() - 0.5) * 0.75;
            double delta = drift + wave + noise;
            double open = price;
            double close = price + delta;
            double high = Math.max(open, close) + (0.15 + random.nextDouble() * 0.85);
            double low = Math.min(open, close) - (0.15 + random.nextDouble() * 0.85);
            long volume = 200 + random.nextInt(3000);
            long spread = 10 + random.nextInt(25);
            series.add(new Candle(time, open, high, low, close, volume, spread));
            price = close;
            time = time.plus(timeframe.duration());
        }
        return series;
    }

    private void validateRange(int from, int toInclusive) {
        if (candles.isEmpty()) throw new IllegalStateException("series is empty");
        if (from < 0 || toInclusive < from || toInclusive >= candles.size()) {
            throw new IndexOutOfBoundsException("invalid range: [" + from + ", " + toInclusive + "]");
        }
    }
}

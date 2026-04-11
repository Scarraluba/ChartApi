package concrete.goonie.chart.api.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable candle record.
 */
public record Candle(
        LocalDateTime time,
        double open,
        double high,
        double low,
        double close,
        long volume,
        long spread
) {
    public Candle {
        Objects.requireNonNull(time, "time");
        if (high < low) throw new IllegalArgumentException("high cannot be lower than low");
        if (open < low || open > high) throw new IllegalArgumentException("open must be inside low/high");
        if (close < low || close > high) throw new IllegalArgumentException("close must be inside low/high");
        if (volume < 0) throw new IllegalArgumentException("volume cannot be negative");
        if (spread < 0) throw new IllegalArgumentException("spread cannot be negative");
    }

    /** Returns true when the candle closed at or above its open. */
    public boolean bullish() {
        return close >= open;
    }
}

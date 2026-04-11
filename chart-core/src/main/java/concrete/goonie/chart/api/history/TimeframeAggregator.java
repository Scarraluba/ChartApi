package concrete.goonie.chart.api.history;

import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Aggregates lower timeframe candles into higher timeframe candles.
 */
public final class TimeframeAggregator {
    private TimeframeAggregator() {}

    public static CandleSeries aggregate(CandleSeries source, Timeframe sourceTimeframe, Timeframe targetTimeframe) {
        if (sourceTimeframe == targetTimeframe) {
            CandleSeries copy = new CandleSeries();
            copy.addAll(source.asList());
            return copy;
        }
        if (!targetTimeframe.isSameOrHigherThan(sourceTimeframe)) {
            throw new IllegalArgumentException("target timeframe must be the same as or higher than source timeframe");
        }

        CandleSeries result = new CandleSeries();
        Candle current = null;
        LocalDateTime bucket = null;

        for (Candle candle : source.asList()) {
            LocalDateTime candleBucket = align(candle.time(), targetTimeframe);
            if (bucket == null || !bucket.equals(candleBucket)) {
                if (current != null) result.add(current);
                bucket = candleBucket;
                current = new Candle(bucket, candle.open(), candle.high(), candle.low(), candle.close(), candle.volume(), candle.spread());
            } else {
                current = new Candle(
                        bucket,
                        current.open(),
                        Math.max(current.high(), candle.high()),
                        Math.min(current.low(), candle.low()),
                        candle.close(),
                        current.volume() + candle.volume(),
                        Math.max(current.spread(), candle.spread())
                );
            }
        }

        if (current != null) result.add(current);
        return result;
    }

    public static LocalDateTime align(LocalDateTime time, Timeframe timeframe) {
        return switch (timeframe) {
            case M1 -> time.truncatedTo(ChronoUnit.MINUTES);
            case M5 -> time.withMinute((time.getMinute() / 5) * 5).withSecond(0).withNano(0);
            case M15 -> time.withMinute((time.getMinute() / 15) * 15).withSecond(0).withNano(0);
            case M30 -> time.withMinute((time.getMinute() / 30) * 30).withSecond(0).withNano(0);
            case H1 -> time.withMinute(0).withSecond(0).withNano(0);
            case H4 -> time.withHour((time.getHour() / 4) * 4).withMinute(0).withSecond(0).withNano(0);
            case D1 -> time.toLocalDate().atStartOfDay();
        };
    }
}

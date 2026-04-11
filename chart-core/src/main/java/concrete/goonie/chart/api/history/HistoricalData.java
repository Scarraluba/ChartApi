package concrete.goonie.chart.api.history;

import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory historical data store with multi-timeframe support.
 */
public final class HistoricalData {
    private final Map<String, Map<Timeframe, CandleSeries>> data = new LinkedHashMap<>();

    public void addCandle(String symbol, Timeframe timeframe, Candle candle) {
        series(symbol, timeframe).add(candle);
    }

    public void setSeries(String symbol, Timeframe timeframe, CandleSeries series) {
        data.computeIfAbsent(symbol, key -> new LinkedHashMap<>()).put(timeframe, series);
    }

    public CandleSeries getSeries(String symbol, Timeframe timeframe) {
        Map<Timeframe, CandleSeries> timeframes = data.get(symbol);
        return timeframes == null ? null : timeframes.get(timeframe);
    }

    public CandleSeries series(String symbol, Timeframe timeframe) {
        return data.computeIfAbsent(symbol, key -> new LinkedHashMap<>()).computeIfAbsent(timeframe, key -> new CandleSeries());
    }

    public Set<String> getSymbols() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(data.keySet()));
    }

    public Set<Timeframe> getAvailableTimeframes(String symbol) {
        Map<Timeframe, CandleSeries> timeframes = data.get(symbol);
        if (timeframes == null) return Collections.emptySet();
        return Collections.unmodifiableSet(new LinkedHashSet<>(timeframes.keySet()));
    }

    public CandleSeries getOrAggregate(String symbol, Timeframe timeframe) {
        CandleSeries direct = getSeries(symbol, timeframe);
        if (direct != null && !direct.isEmpty()) return direct;

        Timeframe sourceTimeframe = null;
        for (Timeframe candidate : getAvailableTimeframes(symbol)) {
            if (!timeframe.isSameOrHigherThan(candidate)) continue;
            if (sourceTimeframe == null || candidate.seconds() > sourceTimeframe.seconds()) {
                sourceTimeframe = candidate;
            }
        }
        if (sourceTimeframe == null || sourceTimeframe == timeframe) {
            return direct;
        }
        CandleSeries source = getSeries(symbol, sourceTimeframe);
        if (source == null || source.isEmpty()) return null;
        CandleSeries aggregated = TimeframeAggregator.aggregate(source, sourceTimeframe, timeframe);
        setSeries(symbol, timeframe, aggregated);
        return aggregated;
    }

    public LocalDateTime[] copyTime(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new LocalDateTime[0] : series.copyTime();
    }

    public double[] copyOpen(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new double[0] : series.copyOpen();
    }

    public double[] copyHigh(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new double[0] : series.copyHigh();
    }

    public double[] copyLow(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new double[0] : series.copyLow();
    }

    public double[] copyClose(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new double[0] : series.copyClose();
    }

    public long[] copyVolume(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new long[0] : series.copyVolume();
    }

    public long[] copySpread(String symbol, Timeframe timeframe) {
        CandleSeries series = getOrAggregate(symbol, timeframe);
        return series == null ? new long[0] : series.copySpread();
    }
}

package concrete.goonie.chart.api.indicator;

import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.model.Timeframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable indicator collection.
 */
public final class IndicatorSet {
    private final List<Indicator> indicators = new ArrayList<>();
    private long mutationVersion;

    public void add(Indicator indicator) {
        if (indicator != null && !indicators.contains(indicator)) {
            indicators.add(indicator);
            mutationVersion++;
        }
    }

    public void remove(Indicator indicator) {
        if (indicators.remove(indicator)) {
            mutationVersion++;
        }
    }

    public void clear() {
        if (!indicators.isEmpty()) {
            indicators.clear();
            mutationVersion++;
        }
    }

    public List<Indicator> all() {
        return Collections.unmodifiableList(indicators);
    }

    public long getMutationVersion() {
        return mutationVersion;
    }

    public void calculateAll(HistoricalData historicalData, String symbol) {
        for (Indicator indicator : indicators) {
            indicator.calculate(historicalData, symbol);
        }
    }

    public List<Indicator> renderableFor(Timeframe timeframe) {
        List<Indicator> out = new ArrayList<>();
        for (Indicator indicator : indicators) {
            if (indicator.supports(timeframe)) out.add(indicator);
        }
        return out;
    }
}

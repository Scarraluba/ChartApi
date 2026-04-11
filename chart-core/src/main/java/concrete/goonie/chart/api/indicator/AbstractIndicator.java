package concrete.goonie.chart.api.indicator;

import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for indicators that use the MT-style calculation signature.
 */
public abstract class AbstractIndicator implements Indicator {
    private final String name;
    private final Timeframe calculationTimeframe;
    private final Set<Timeframe> selectedTimeframes = new LinkedHashSet<>();
    private final List<IndicatorBuffer> buffers = new ArrayList<>();

    protected AbstractIndicator(String name, Timeframe calculationTimeframe, Timeframe... selectedTimeframes) {
        this.name = Objects.requireNonNull(name, "name");
        this.calculationTimeframe = Objects.requireNonNull(calculationTimeframe, "calculationTimeframe");
        if (selectedTimeframes != null && selectedTimeframes.length > 0) {
            this.selectedTimeframes.addAll(Arrays.asList(selectedTimeframes));
        }
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final Timeframe getCalculationTimeframe() {
        return calculationTimeframe;
    }

    @Override
    public final boolean supports(Timeframe timeframe) {
        return selectedTimeframes.isEmpty() || selectedTimeframes.contains(timeframe);
    }

    protected final void addBuffer(IndicatorBuffer buffer) {
        buffers.add(buffer);
    }

    protected final IndicatorBuffer buffer(int index) {
        return buffers.get(index);
    }

    protected final List<IndicatorBuffer> getBuffers() {
        return Collections.unmodifiableList(buffers);
    }

    protected final void clearBuffers() {
        buffers.clear();
    }

    protected final void ensureBufferCount(int expected) {
        if (buffers.size() != expected) {
            throw new IllegalStateException("Expected " + expected + " buffers but found " + buffers.size());
        }
    }

    @Override
    public final void calculate(HistoricalData historicalData, String symbol) {
        CandleSeries series = historicalData.getOrAggregate(symbol, calculationTimeframe);
        if (series == null || series.isEmpty()) return;
        prepareBuffers(series.size());
        calculateIndicator(
                series.size(),
                0,
                series.copyTime(),
                series.copyOpen(),
                series.copyHigh(),
                series.copyLow(),
                series.copyClose(),
                series.copyVolume(),
                series.copySpread()
        );
    }

    protected abstract void prepareBuffers(int size);

    protected abstract void calculateIndicator(int rates_total, int prev_calculated, LocalDateTime[] time,
                                              double[] open, double[] high, double[] low, double[] close,
                                              long[] volume, long[] spread);

    @Override
    public final List<DataSet> getRenderableDataSets(Timeframe chartTimeframe) {
        if (!supports(chartTimeframe)) return List.of();
        List<DataSet> out = new ArrayList<>();
        for (IndicatorBuffer buffer : buffers) {
            out.add(buffer.toDataSet(name, chartTimeframe));
        }
        return out;
    }
}

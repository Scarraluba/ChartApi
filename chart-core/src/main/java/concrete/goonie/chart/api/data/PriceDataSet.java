package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;

import java.util.Objects;

/**
 * Dataset backed by a candle series.
 */
public abstract class PriceDataSet extends DataSet {
    private CandleSeries series;

    protected PriceDataSet(String name, DataSetType type, CandleSeries series, Timeframe... timeframes) {
        super(name, type, timeframes);
        this.series = Objects.requireNonNull(series, "series");
    }

    public final CandleSeries getSeries() {
        return series;
    }

    public final void setSeries(CandleSeries series) {
        this.series = Objects.requireNonNull(series, "series");
    }
}

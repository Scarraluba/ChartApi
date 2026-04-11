package concrete.goonie.chart.api.indicator;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.Style;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simple moving average indicator.
 */
public final class SmaIndicator extends AbstractIndicator implements ConfigurableIndicator {
    private int period;

    public SmaIndicator(String name, Timeframe calculationTimeframe, int period, Timeframe... selectedTimeframes) {
        super(name, calculationTimeframe, selectedTimeframes);
        if (period < 1) throw new IllegalArgumentException("period must be at least 1");
        this.period = period;
    }


    public int getPeriod() {
        return period;
    }

    @Override
    public List<IndicatorProperty> getProperties() {
        return List.of(
                new IndicatorProperty("period", "Period", IndicatorPropertyType.INTEGER, Integer.toString(period), true),
                new IndicatorProperty("timeframe", "Calculation TF", IndicatorPropertyType.TEXT, getCalculationTimeframe().name(), false)
        );
    }

    @Override
    public boolean applyProperty(String key, String rawValue) {
        if (key == null) {
            return false;
        }
        if ("period".equals(key)) {
            try {
                int next = Integer.parseInt(rawValue == null ? "" : rawValue.trim());
                if (next < 1) {
                    return false;
                }
                period = next;
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected void prepareBuffers(int size) {
        if (getBuffers().isEmpty()) {
            addBuffer(new IndicatorBuffer(getName(), IndicatorBufferRenderType.LINE, size, new Style(0xFF38BDF8, 2f, false)));
        }
        if (buffer(0).getValues().length != size) {
            clearBuffers();
        }
        if (getBuffers().isEmpty()) {
            addBuffer(new IndicatorBuffer(getName(), IndicatorBufferRenderType.LINE, size, new Style(0xFF38BDF8, 2f, false)));
        }
        buffer(0).clear();
    }

    @Override
    protected void calculateIndicator(int rates_total, int prev_calculated, LocalDateTime[] time,
                                      double[] open, double[] high, double[] low, double[] close,
                                      long[] volume, long[] spread) {
        IndicatorBuffer out = buffer(0);
        for (int i = 0; i < rates_total; i++) {
            if (i + 1 < period) {
                out.set(i, Double.NaN);
                continue;
            }
            double sum = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += close[j];
            }
            out.set(i, sum / period);
        }
    }
}

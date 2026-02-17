package concrete.goonie.chart.api.indicator;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.Style;

import java.time.LocalDateTime;

/**
 * Volume histogram example indicator.
 */
public final class VolumeHistogramIndicator extends AbstractIndicator {
    public VolumeHistogramIndicator(String name, Timeframe calculationTimeframe, Timeframe... selectedTimeframes) {
        super(name, calculationTimeframe, selectedTimeframes);
    }

    @Override
    protected void prepareBuffers(int size) {
        if (!getBuffers().isEmpty() && buffer(0).getValues().length == size) {
            buffer(0).clear();
            return;
        }
        clearBuffers();
        IndicatorBuffer buffer = new IndicatorBuffer(getName(), IndicatorBufferRenderType.HISTOGRAM, size, new Style(0x668B5CF6, 1f, true));
        buffer.setHistogramBaseValue(0.0);
        addBuffer(buffer);
    }

    @Override
    protected void calculateIndicator(int rates_total, int prev_calculated, LocalDateTime[] time,
                                      double[] open, double[] high, double[] low, double[] close,
                                      long[] volume, long[] spread) {
        IndicatorBuffer out = buffer(0);
        for (int i = 0; i < rates_total; i++) {
            out.set(i, volume[i]);
        }
    }
}

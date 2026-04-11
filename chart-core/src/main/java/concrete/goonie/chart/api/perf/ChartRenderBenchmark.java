package concrete.goonie.chart.api.perf;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

import java.time.LocalDateTime;

/**
 * Tiny benchmark harness for render command generation.
 */
public final class ChartRenderBenchmark {
    private ChartRenderBenchmark() {}

    public static String runCandlesBenchmark(int candleCount, int width, int height, int loops) {
        CandleSeries series = CandleSeries.generateDemo(LocalDateTime.of(2025, 1, 1, 0, 0), Timeframe.M1, candleCount);
        CandlestickDataSet dataSet = new CandlestickDataSet("Benchmark", series, Timeframe.M1);
        Viewport viewport = new Viewport(Math.max(0, series.size() - Math.min(series.size(), width * 8.0)), Math.max(1, series.size() - 1), series.minLow(Math.max(0, series.size() - Math.min(series.size(), width * 8)), series.size() - 1), series.maxHigh(Math.max(0, series.size() - Math.min(series.size(), width * 8)), series.size() - 1));
        Transform transform = new Transform(0, 0, width, height, viewport);
        RenderList renderList = new RenderList();
        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            renderList.clear();
            dataSet.render(renderList, transform, (int) viewport.getXMin(), (int) viewport.getXMax());
        }
        long elapsed = System.nanoTime() - start;
        double avgMs = elapsed / 1_000_000.0 / Math.max(1, loops);
        return "candles=" + candleCount + ", loops=" + loops + ", commands=" + renderList.size() + ", avgMs=" + String.format(java.util.Locale.US, "%.3f", avgMs);
    }
}

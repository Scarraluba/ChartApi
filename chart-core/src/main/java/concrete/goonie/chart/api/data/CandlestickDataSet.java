package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.data.render.CandlestickDataSetRenderer;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

/**
 * Main price dataset rendered as candlesticks.
 */
public final class CandlestickDataSet extends PriceDataSet {
    private final CandlestickDataSetRenderer renderer = new CandlestickDataSetRenderer();
    private Style wickStyle = new Style(0x78FFFFFF, 1.3f, false);
    private Style bullBodyStyle = new Style(0xDC22C55E, 1f, true);
    private Style bearBodyStyle = new Style(0xDCEF4444, 1f, true);
    private concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions = new concrete.goonie.chart.api.perf.RenderOptimizationOptions();

    public CandlestickDataSet(String name, CandleSeries series, Timeframe... timeframes) {
        super(name, DataSetType.CANDLESTICK, series, timeframes);
    }

    public Style getWickStyle() { return wickStyle; }
    public Style getBullBodyStyle() { return bullBodyStyle; }
    public Style getBearBodyStyle() { return bearBodyStyle; }
    public void setWickStyle(Style wickStyle) { this.wickStyle = wickStyle; }
    public void setBullBodyStyle(Style bullBodyStyle) { this.bullBodyStyle = bullBodyStyle; }
    public void setBearBodyStyle(Style bearBodyStyle) { this.bearBodyStyle = bearBodyStyle; }
    public concrete.goonie.chart.api.perf.RenderOptimizationOptions getOptimizationOptions() { return optimizationOptions; }
    public void setOptimizationOptions(concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions) { this.optimizationOptions = optimizationOptions == null ? new concrete.goonie.chart.api.perf.RenderOptimizationOptions() : optimizationOptions; }

    @Override
    public void render(RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        renderer.render(this, out, transform, visibleFrom, visibleTo);
    }
}

package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.data.render.BarDataSetRenderer;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

/**
 * Bar dataset for histogram-like data.
 */
public final class BarDataSet extends ValueDataSet {
    private final BarDataSetRenderer renderer = new BarDataSetRenderer();
    private Style barStyle = new Style(0xAA8B5CF6, 1f, true);
    private concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions = new concrete.goonie.chart.api.perf.RenderOptimizationOptions();
    private double baseValue;

    public BarDataSet(String name, double[] xValues, double[] yValues, double baseValue, Timeframe... timeframes) {
        super(name, DataSetType.BAR, xValues, yValues, timeframes);
        this.baseValue = baseValue;
    }

    public Style getBarStyle() { return barStyle; }
    public void setBarStyle(Style barStyle) { this.barStyle = barStyle; }
    public concrete.goonie.chart.api.perf.RenderOptimizationOptions getOptimizationOptions() { return optimizationOptions; }
    public void setOptimizationOptions(concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions) { this.optimizationOptions = optimizationOptions == null ? new concrete.goonie.chart.api.perf.RenderOptimizationOptions() : optimizationOptions; }
    public double getBaseValue() { return baseValue; }
    public void setBaseValue(double baseValue) { this.baseValue = baseValue; }
    public int sizeValue() { return size(); }
    public double xValue(int index) { return xAt(index); }
    public double yValue(int index) { return yAt(index); }

    @Override
    public void render(RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        renderer.render(this, out, transform, visibleFrom, visibleTo);
    }
}

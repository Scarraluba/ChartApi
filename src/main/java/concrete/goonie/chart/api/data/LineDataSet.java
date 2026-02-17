package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.data.render.LineDataSetRenderer;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

/**
 * Line dataset for overlays such as custom series.
 */
public final class LineDataSet extends ValueDataSet {
    private final LineDataSetRenderer renderer = new LineDataSetRenderer();
    private Style lineStyle = new Style(0xFF60A5FA, 2f, false);
    private concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions = new concrete.goonie.chart.api.perf.RenderOptimizationOptions();

    public LineDataSet(String name, double[] xValues, double[] yValues, Timeframe... timeframes) {
        super(name, DataSetType.LINE, xValues, yValues, timeframes);
    }

    public Style getLineStyle() { return lineStyle; }
    public void setLineStyle(Style lineStyle) { this.lineStyle = lineStyle; }
    public concrete.goonie.chart.api.perf.RenderOptimizationOptions getOptimizationOptions() { return optimizationOptions; }
    public void setOptimizationOptions(concrete.goonie.chart.api.perf.RenderOptimizationOptions optimizationOptions) { this.optimizationOptions = optimizationOptions == null ? new concrete.goonie.chart.api.perf.RenderOptimizationOptions() : optimizationOptions; }
    public int sizeValue() { return size(); }
    public double xValue(int index) { return xAt(index); }
    public double yValue(int index) { return yAt(index); }

    @Override
    public void render(RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        renderer.render(this, out, transform, visibleFrom, visibleTo);
    }
}

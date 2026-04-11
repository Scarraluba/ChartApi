package concrete.goonie.chart.api.control;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.api.render.ChartEngine;
import concrete.goonie.chart.api.render.AxisRangePolicy;
import concrete.goonie.chart.internal.render.Transform;

/**
 * Handles pan, zoom, and auto-fit Y behaviour.
 */
public final class ViewportController {
    private final ChartEngine chartEngine;
    private double minXRange = 10;
    private double minYRange = 0.5;
    private double maxYRange = 1e9;
    private AxisRangePolicy xAxisRangePolicy = new AxisRangePolicy();
    private AxisRangePolicy yAxisRangePolicy = new AxisRangePolicy();

    public ViewportController(ChartEngine chartEngine) {
        this.chartEngine = chartEngine;
    }

    public ViewportController setXAxisRangePolicy(AxisRangePolicy xAxisRangePolicy) {
        this.xAxisRangePolicy = xAxisRangePolicy == null ? new AxisRangePolicy() : xAxisRangePolicy;
        return this;
    }

    public ViewportController setYAxisRangePolicy(AxisRangePolicy yAxisRangePolicy) {
        this.yAxisRangePolicy = yAxisRangePolicy == null ? new AxisRangePolicy() : yAxisRangePolicy;
        return this;
    }

    public void panByPixels(CandleSeries series, Viewport viewport, ChartEngine.Params params, float dxPixels, float dyPixels) {
        if (!isValid(series, viewport, params)) return;
        ChartEngine.ChartRect rect = chartEngine.computeChartRect(params);
        Transform transform = new Transform(rect.left(), rect.top(), rect.width(), rect.height(), viewport);
        double dxWorld = -dxPixels / transform.pixelsPerWorldX();
        double dyWorld = dyPixels / transform.pixelsPerWorldY();
        viewport.translate(dxWorld, dyWorld);
        viewport.applyXPolicy(xAxisRangePolicy);
        viewport.clampX(0, Math.max(1, series.size() - 1));
        viewport.applyYPolicy(yAxisRangePolicy);
    }

    public void zoomAtScreen(CandleSeries series, Viewport viewport, ChartEngine.Params params, float x, float y, double factor, boolean zoomX, boolean zoomY) {
        if (!isValid(series, viewport, params)) return;
        ChartEngine.ChartRect rect = chartEngine.computeChartRect(params);
        Transform transform = new Transform(rect.left(), rect.top(), rect.width(), rect.height(), viewport);
        double anchorX = transform.screenToWorldX(x);
        double anchorY = transform.screenToWorldY(y);
        if (zoomX) zoomXAt(series, viewport, anchorX, factor);
        if (zoomY) zoomYAt(viewport, anchorY, factor);
        viewport.applyXPolicy(xAxisRangePolicy);
        viewport.clampX(0, Math.max(1, series.size() - 1));
        viewport.applyYPolicy(yAxisRangePolicy);
    }

    public void autoFitY(CandleSeries series, Viewport viewport, double paddingFraction) {
        if (series == null || series.isEmpty() || viewport == null) return;
        int from = Math.max(0, (int) Math.floor(viewport.getXMin()));
        int to = Math.min(series.size() - 1, (int) Math.ceil(viewport.getXMax()));
        if (to < from) return;
        double min = series.minLow(from, to);
        double max = series.maxHigh(from, to);
        double pad = Math.max(1e-9, (max - min) * Math.max(0.0, paddingFraction));
        if (Math.abs(max - min) <= 1e-9) {
            pad = Math.max(1.0, Math.abs(max) * 0.01);
        }
        viewport.setY(min - pad, max + pad, yAxisRangePolicy);
    }

    public void autoFitY(CandlestickDataSet dataSet, Viewport viewport, double paddingFraction) {
        if (dataSet == null) return;
        autoFitY(dataSet.getSeries(), viewport, paddingFraction);
    }

    private void zoomXAt(CandleSeries series, Viewport viewport, double anchorX, double factor) {
        double oldRange = Math.max(1e-9, viewport.xRange());
        double maxXRange = Math.max(50.0, series.size());
        double newRange = clamp(oldRange * factor, minXRange, maxXRange);
        double ratio = (anchorX - viewport.getXMin()) / oldRange;
        double xMin = anchorX - ratio * newRange;
        viewport.setX(xMin, xMin + newRange, xAxisRangePolicy);
    }

    private void zoomYAt(Viewport viewport, double anchorY, double factor) {
        double oldRange = Math.max(1e-9, viewport.yRange());
        double newRange = clamp(oldRange * factor, minYRange, maxYRange);
        double ratio = (anchorY - viewport.getYMin()) / oldRange;
        double yMin = anchorY - ratio * newRange;
        viewport.setY(yMin, yMin + newRange, yAxisRangePolicy);
    }

    private boolean isValid(CandleSeries series, Viewport viewport, ChartEngine.Params params) {
        return series != null && series.size() >= 2 && viewport != null && params != null && params.getViewWidthPx() > 0 && params.getViewHeightPx() > 0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

package concrete.goonie.chart.api.perf;

/**
 * Performance options for large-series rendering.
 */
public final class RenderOptimizationOptions {
    private boolean enabled = true;
    private boolean candleDecimationEnabled = true;
    private boolean lineDecimationEnabled = true;
    private boolean barDecimationEnabled = true;
    private boolean retainedSceneCachingEnabled = true;
    private int targetCandlesPerPixel = 2;
    private int targetLineSegmentsPerPixel = 2;
    private int targetBarsPerPixel = 2;

    public boolean isEnabled() { return enabled; }
    public RenderOptimizationOptions setEnabled(boolean enabled) { this.enabled = enabled; return this; }
    public boolean isCandleDecimationEnabled() { return candleDecimationEnabled; }
    public RenderOptimizationOptions setCandleDecimationEnabled(boolean candleDecimationEnabled) { this.candleDecimationEnabled = candleDecimationEnabled; return this; }
    public boolean isLineDecimationEnabled() { return lineDecimationEnabled; }
    public RenderOptimizationOptions setLineDecimationEnabled(boolean lineDecimationEnabled) { this.lineDecimationEnabled = lineDecimationEnabled; return this; }
    public boolean isBarDecimationEnabled() { return barDecimationEnabled; }
    public RenderOptimizationOptions setBarDecimationEnabled(boolean barDecimationEnabled) { this.barDecimationEnabled = barDecimationEnabled; return this; }
    public boolean isRetainedSceneCachingEnabled() { return retainedSceneCachingEnabled; }
    public RenderOptimizationOptions setRetainedSceneCachingEnabled(boolean retainedSceneCachingEnabled) { this.retainedSceneCachingEnabled = retainedSceneCachingEnabled; return this; }
    public int getTargetCandlesPerPixel() { return Math.max(1, targetCandlesPerPixel); }
    public RenderOptimizationOptions setTargetCandlesPerPixel(int targetCandlesPerPixel) { this.targetCandlesPerPixel = Math.max(1, targetCandlesPerPixel); return this; }
    public int getTargetLineSegmentsPerPixel() { return Math.max(1, targetLineSegmentsPerPixel); }
    public RenderOptimizationOptions setTargetLineSegmentsPerPixel(int targetLineSegmentsPerPixel) { this.targetLineSegmentsPerPixel = Math.max(1, targetLineSegmentsPerPixel); return this; }
    public int getTargetBarsPerPixel() { return Math.max(1, targetBarsPerPixel); }
    public RenderOptimizationOptions setTargetBarsPerPixel(int targetBarsPerPixel) { this.targetBarsPerPixel = Math.max(1, targetBarsPerPixel); return this; }
}

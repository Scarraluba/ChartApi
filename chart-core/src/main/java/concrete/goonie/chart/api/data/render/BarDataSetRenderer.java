package concrete.goonie.chart.api.data.render;

import concrete.goonie.chart.api.data.BarDataSet;
import concrete.goonie.chart.api.perf.RenderOptimizationOptions;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

/** Renders bar datasets. */
public final class BarDataSetRenderer {
    public void render(BarDataSet dataSet, RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        int from = Math.max(0, visibleFrom);
        int to = Math.min(dataSet.sizeValue() - 1, visibleTo);
        if (to < from) return;

        double pxPerIndex = transform.pixelsPerWorldX();
        int barWidth = Math.max(2, Math.min(14, (int) Math.floor(pxPerIndex * 0.60)));
        double baseValue = dataSet.getBaseValue();
        int baseY = transform.worldToScreenY(baseValue);
        RenderOptimizationOptions opt = dataSet.getOptimizationOptions();
        int visibleCount = to - from + 1;
        int maxBars = Math.max(1, transform.width() * Math.max(1, opt.getTargetBarsPerPixel()));
        int bucketSize = opt.isEnabled() && opt.isBarDecimationEnabled() && visibleCount > maxBars
                ? (int) Math.ceil(visibleCount / (double) maxBars)
                : 1;

        for (int start = from; start <= to; start += bucketSize) {
            int end = Math.min(to, start + bucketSize - 1);
            double x = dataSet.xValue((start + end) / 2);
            double y = 0.0;
            for (int i = start; i <= end; i++) {
                y += dataSet.yValue(i);
            }
            y /= Math.max(1, end - start + 1);
            int sx = transform.worldToScreenX(x);
            int sy = transform.worldToScreenY(y);
            int top = Math.min(baseY, sy);
            int bottom = Math.max(baseY, sy);
            int width = Math.max(barWidth, Math.min(transform.width(), barWidth * bucketSize));
            out.add(new RenderCommand.Rect(sx - width / 2, top, sx + width / 2, bottom, dataSet.getBarStyle()));
        }
    }
}

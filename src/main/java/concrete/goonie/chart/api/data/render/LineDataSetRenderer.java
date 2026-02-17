package concrete.goonie.chart.api.data.render;

import concrete.goonie.chart.api.data.LineDataSet;
import concrete.goonie.chart.api.perf.RenderOptimizationOptions;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

/** Renders line datasets. */
public final class LineDataSetRenderer {
    public void render(LineDataSet dataSet, RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        int from = Math.max(0, visibleFrom);
        int to = Math.min(dataSet.sizeValue() - 1, visibleTo);
        if (to <= from) return;

        RenderOptimizationOptions opt = dataSet.getOptimizationOptions();
        int visibleCount = to - from + 1;
        int maxSegments = Math.max(1, transform.width() * Math.max(1, opt.getTargetLineSegmentsPerPixel()));
        int bucketSize = opt.isEnabled() && opt.isLineDecimationEnabled() && visibleCount > maxSegments
                ? (int) Math.ceil(visibleCount / (double) maxSegments)
                : 1;

        if (bucketSize == 1) {
            for (int i = from + 1; i <= to; i++) {
                out.add(new RenderCommand.Line(
                        transform.worldToScreenX(dataSet.xValue(i - 1)),
                        transform.worldToScreenY(dataSet.yValue(i - 1)),
                        transform.worldToScreenX(dataSet.xValue(i)),
                        transform.worldToScreenY(dataSet.yValue(i)),
                        dataSet.getLineStyle()
                ));
            }
            return;
        }

        double prevX = dataSet.xValue(from);
        double prevY = dataSet.yValue(from);
        for (int start = from + bucketSize; start <= to; start += bucketSize) {
            int end = Math.min(to, start + bucketSize - 1);
            double bucketX = dataSet.xValue(end);
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (int i = start; i <= end; i++) {
                double y = dataSet.yValue(i);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            out.add(new RenderCommand.Line(transform.worldToScreenX(prevX), transform.worldToScreenY(prevY), transform.worldToScreenX(bucketX), transform.worldToScreenY(minY), dataSet.getLineStyle()));
            if (maxY != minY) {
                out.add(new RenderCommand.Line(transform.worldToScreenX(bucketX), transform.worldToScreenY(minY), transform.worldToScreenX(bucketX), transform.worldToScreenY(maxY), dataSet.getLineStyle()));
            }
            prevX = bucketX;
            prevY = dataSet.yValue(end);
        }
    }
}

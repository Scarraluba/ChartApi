package concrete.goonie.chart.api.data.render;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.perf.RenderOptimizationOptions;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

/** Renders candlestick datasets. */
public final class CandlestickDataSetRenderer {
    public void render(CandlestickDataSet dataSet, RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        int from = Math.max(0, visibleFrom);
        int to = Math.min(dataSet.getSeries().size() - 1, visibleTo);
        if (to < from) return;

        double pxPerIndex = transform.pixelsPerWorldX();
        Style wick = dataSet.getWickStyle();
        Style bull = dataSet.getBullBodyStyle();
        Style bear = dataSet.getBearBodyStyle();
        RenderOptimizationOptions opt = dataSet.getOptimizationOptions();
        int visibleCount = to - from + 1;
        int maxCandles = Math.max(1, transform.width() * Math.max(1, opt.getTargetCandlesPerPixel()));
        int bucketSize = opt.isEnabled() && opt.isCandleDecimationEnabled() && visibleCount > maxCandles
                ? (int) Math.ceil(visibleCount / (double) maxCandles)
                : 1;

        for (int start = from; start <= to; start += bucketSize) {
            int end = Math.min(to, start + bucketSize - 1);
            Candle candle = bucketSize == 1 ? dataSet.getSeries().get(start) : aggregate(dataSet, start, end);
            int x = transform.worldToScreenX((start + end) * 0.5);
            int yOpen = transform.worldToScreenY(candle.open());
            int yClose = transform.worldToScreenY(candle.close());
            int yHigh = transform.worldToScreenY(candle.high());
            int yLow = transform.worldToScreenY(candle.low());
            int top = Math.min(yOpen, yClose);
            int bottom = Math.max(yOpen, yClose);
            out.add(new RenderCommand.Line(x, yHigh, x, top, wick));

            int slotPixelWidth = slotPixelWidth(pxPerIndex, bucketSize);
            int bodyWidth = computeOddBodyWidth(slotPixelWidth);
            int left = x - (bodyWidth / 2);
            int height = Math.max(1, bottom - top);
            int y1 = top + height;
            out.add(new RenderCommand.Line(x, y1, x, yLow, wick));
            out.add(new RenderCommand.Rect(left, top, left + bodyWidth, y1, candle.bullish() ? bull : bear));
        }
    }

    private static int slotPixelWidth(double pxPerIndex, int bucketSize) {
        return Math.max(1, (int) Math.round(Math.max(1.0, pxPerIndex) * Math.max(1, bucketSize)));
    }

    private static int computeOddBodyWidth(int slotPixelWidth) {
        int gap = preferredGap(slotPixelWidth);
        int width = Math.max(1, slotPixelWidth - gap);
        if ((width & 1) == 0) {
            width -= 1;
        }
        if (width < 1) {
            width = 1;
        }
        return width;
    }

    private static int preferredGap(int slotPixelWidth) {
        if (slotPixelWidth <= 2) return 1;
        if (slotPixelWidth <= 6) return 1;
        if (slotPixelWidth <= 12) return 2;
        if (slotPixelWidth <= 20) return 3;
        return 4;
    }

    private Candle aggregate(CandlestickDataSet dataSet, int from, int to) {
        Candle first = dataSet.getSeries().get(from);
        Candle last = dataSet.getSeries().get(to);
        double high = first.high();
        double low = first.low();
        long volume = 0L;
        long spread = 0L;
        for (int i = from; i <= to; i++) {
            Candle candle = dataSet.getSeries().get(i);
            high = Math.max(high, candle.high());
            low = Math.min(low, candle.low());
            volume += candle.volume();
            spread += candle.spread();
        }
        return new Candle(first.time(), first.open(), high, low, last.close(), volume, spread / Math.max(1, (to - from + 1)));
    }
}

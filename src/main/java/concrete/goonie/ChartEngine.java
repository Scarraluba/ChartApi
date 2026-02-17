package concrete.goonie;

import java.util.List;
import java.util.Locale;

public final class ChartEngine {

    // ---- Theme styles (pure numbers; renderer maps them) ----
    public static final int COLOR_BG = 0xFF111827;          // #111827
    public static final int COLOR_GRID_MINOR = 0x12FFFFFF;  // alpha 0x12
    public static final int COLOR_GRID_MAJOR = 0x28FFFFFF;  // alpha 0x28
    public static final int COLOR_AXIS = 0x3CFFFFFF;        // alpha 0x3C
    public static final int COLOR_TEXT = 0xDDE5E7EB;        // light gray-ish
    public static final int COLOR_WICK = 0x78FFFFFF;        // alpha 0x78
    public static final int COLOR_UP = 0xDC22C55E;          // green-ish
    public static final int COLOR_DOWN = 0xDCEF4444;        // red-ish
    public static final int COLOR_CROSS = 0x46FFFFFF;       // alpha 0x46
    private  Params p;
    public static final class Params {
        // layout in pixels (platform gives these)
        public int viewWidthPx;
        public int viewHeightPx;

        // chart padding in pixels
        public int padLeft = 60;
        public int padTop = 18;
        public int padRight = 16;
        public int padBottom = 36;

        // candle width clamp
        public int minBodyW = 2;
        public int maxBodyW = 18;

        // label sizes (px-like)
        public float labelTextSize = 12f;

        // crosshair (optional)
        public boolean drawCrosshair = false;
        public float crosshairScreenX = -1;
        public float crosshairScreenY = -1;

        // show axis labels
        public boolean drawAxisLabels = true;
        public int gridMinorDivisions =1;
    }

    public static final class ChartRect {
        public final int left, top, width, height;
        public ChartRect(int left, int top, int width, int height) {
            this.left = left; this.top = top; this.width = width; this.height = height;
        }
        public int right() { return left + width; }
        public int bottom() { return top + height; }
    }

    public ChartRect computeChartRect(Params p) {
        this.p = p;
        int left = p.padLeft;
        int top = p.padTop;
        int right = Math.max(left + 1, p.viewWidthPx - p.padRight);
        int bottom = Math.max(top + 1, p.viewHeightPx - p.padBottom);
        return new ChartRect(left, top, right - left, bottom - top);
    }

    public void build(RenderList out, CandleSeries series, Viewport vp, Params p) {
        this.p = p;
        out.clear();
        if (series == null || vp == null || series.size() < 2) return;

        ChartRect cr = computeChartRect(p);
        Transform t = new Transform(cr.left, cr.top, cr.width, cr.height, vp);

        // 1) Grid
        addGrid(out, vp, t);

        // 2) Axes lines
        addAxes(out, t);

        // 3) Axis labels (major lines)
        if (p.drawAxisLabels) {
            addAxisLabels(out, vp, t, p.labelTextSize);
        }

        // 4) Candles
        addCandles(out, series, vp, t, p.minBodyW, p.maxBodyW);

        // 5) Crosshair (optional)
        if (p.drawCrosshair) {
            addCrosshair(out, series, t, p.crosshairScreenX, p.crosshairScreenY, p.labelTextSize);
        }
    }

    private void addGrid(RenderList out, Viewport vp, Transform t) {

        List<GridLines.Line> xs = GridLines.buildX(vp, t.width(), p.gridMinorDivisions);
        List<GridLines.Line> ys = GridLines.buildY(vp, t.height(), p.gridMinorDivisions);

        Style minor = new Style(COLOR_GRID_MINOR, 1f, false);
        Style major = new Style(COLOR_GRID_MAJOR, 1f, false);

        for (GridLines.Line line : xs) {
            int x = t.worldToScreenX(line.worldValue);
            if (x < t.left() || x > t.right()) continue;
            out.add(new RenderCommand.Line(x, t.top(), x, t.bottom(), line.major ? major : minor));
        }

        for (GridLines.Line line : ys) {
            int y = t.worldToScreenY(line.worldValue);
            if (y < t.top() || y > t.bottom()) continue;
            out.add(new RenderCommand.Line(t.left(), y, t.right(), y, line.major ? major : minor));
        }
    }

    private void addAxes(RenderList out, Transform t) {
        Style axis = new Style(COLOR_AXIS, 1.5f, false);
        out.add(new RenderCommand.Line(t.left(), t.top(), t.left(), t.bottom(), axis));
        out.add(new RenderCommand.Line(t.left(), t.bottom(), t.right(), t.bottom(), axis));
    }

    private void addAxisLabels(RenderList out, Viewport vp, Transform t, float textSize) {
        Style textStyle = new Style(COLOR_TEXT, 1f, false);

        // Y labels on major lines
        List<GridLines.Line> ys = GridLines.buildY(vp, t.height(), p.gridMinorDivisions);
        for (GridLines.Line line : ys) {
            if (!line.major) continue;
            int y = t.worldToScreenY(line.worldValue);
            if (y < t.top() || y > t.bottom()) continue;

            String s = formatPrice(line.worldValue);
            // left gutter (renderer can clip if needed)
            out.add(new RenderCommand.Text(8, y + (textSize * 0.35f), s, textSize, textStyle));
        }

        // X labels on major lines (index)
        List<GridLines.Line> xs = GridLines.buildX(vp, t.width(), p.gridMinorDivisions);
        for (GridLines.Line line : xs) {
            if (!line.major) continue;
            int x = t.worldToScreenX(line.worldValue);
            if (x < t.left() || x > t.right()) continue;

            int idx = (int) Math.round(line.worldValue);
            out.add(new RenderCommand.Text(x - 14, t.bottom() + textSize + 10, String.valueOf(idx), textSize, textStyle));
        }
    }

    private void addCandles(RenderList out, CandleSeries series, Viewport vp, Transform t, int minBodyW, int maxBodyW) {
        int i0 = (int) Math.floor(vp.xMin);
        int i1 = (int) Math.ceil(vp.xMax);

        i0 = Math.max(0, i0);
        i1 = Math.min(series.size() - 1, i1);

        double pxPerIndex = t.pixelsPerWorldX();
        int bodyW = (int) Math.max(minBodyW, Math.min(maxBodyW, Math.floor(pxPerIndex * 0.70)));

        Style wick = new Style(COLOR_WICK, 1.3f, false);
        Style up = new Style(COLOR_UP, 1f, true);
        Style dn = new Style(COLOR_DOWN, 1f, true);

        for (int i = i0; i <= i1; i++) {
            Candle c = series.get(i);

            int x = t.worldToScreenX(i);

            int yO = t.worldToScreenY(c.open);
            int yC = t.worldToScreenY(c.close);
            int yH = t.worldToScreenY(c.high);
            int yL = t.worldToScreenY(c.low);

            int top = Math.min(yO, yC);
            int bot = Math.max(yO, yC);

            // wick
            out.add(new RenderCommand.Line(x, yH, x, yL, wick));

            // body
            boolean isUp = c.close >= c.open;
            int bx = x - bodyW / 2;
            int by = top;
            int bh = Math.max(1, bot - top);

            out.add(new RenderCommand.Rect(bx, by, bx + bodyW, by + bh, isUp ? up : dn));
        }
    }

    private void addCrosshair(RenderList out, CandleSeries series, Transform t, float sx, float sy, float textSize) {
        if (sx < t.left() || sx > t.right()) return;
        if (sy < t.top() || sy > t.bottom()) return;

        Style cross = new Style(COLOR_CROSS, 1f, false);

        out.add(new RenderCommand.Line(sx, t.top(), sx, t.bottom(), cross));
        out.add(new RenderCommand.Line(t.left(), sy, t.right(), sy, cross));

        double wx = t.screenToWorldX(sx);
        double wy = t.screenToWorldY(sy);

        int idx = (int) Math.round(wx);
        idx = Math.max(0, Math.min(series.size() - 1, idx));

        Style text = new Style(COLOR_TEXT, 1f, false);

        out.add(new RenderCommand.Text(8, sy - 12, formatPrice(wy), textSize, text));
        out.add(new RenderCommand.Text(sx - 28, t.bottom() + 8, "i " + idx, textSize, text));
    }

    private static String formatPrice(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
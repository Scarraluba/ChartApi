package concrete.goonie.chart.api.render;

import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.internal.render.GridLines;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleFunction;

public final class ChartEngine {
    public record AxisLabel(String text, float textSize, int fontStyle) {}
    public static final class Params {
        private int viewWidthPx;
        private int viewHeightPx;
        private int contentPadLeft = 16;
        private int contentPadTop = 18;
        private int contentPadRight = 16;
        private int contentPadBottom = 16;
        private int axisLabelGapPx = 12;
        private int yAxisLabelWidthPx = 56;
        private int xAxisLabelHeightPx = 24;
        private float labelTextSize = 12f;
        private boolean drawCrosshair;
        private float crosshairScreenX = -1;
        private float crosshairScreenY = -1;
        private boolean drawAxisLabels = true;
        private int gridMinorDivisions = 1;
        private AxisXLocation axisXLocation = AxisXLocation.BOTTOM;
        private AxisYLocation axisYLocation = AxisYLocation.RIGHT;
        private DoubleFunction<String> xAxisLabelFormatter;
        private DoubleFunction<AxisLabel> xAxisLabelProvider;

        public int getViewWidthPx() { return viewWidthPx; }
        public int getViewHeightPx() { return viewHeightPx; }
        public int getContentPadLeft() { return contentPadLeft; }
        public int getContentPadTop() { return contentPadTop; }
        public int getContentPadRight() { return contentPadRight; }
        public int getContentPadBottom() { return contentPadBottom; }
        public int getAxisLabelGapPx() { return axisLabelGapPx; }
        public int getYAxisLabelWidthPx() { return yAxisLabelWidthPx; }
        public int getXAxisLabelHeightPx() { return xAxisLabelHeightPx; }
        public float getLabelTextSize() { return labelTextSize; }
        public boolean isDrawCrosshair() { return drawCrosshair; }
        public float getCrosshairScreenX() { return crosshairScreenX; }
        public float getCrosshairScreenY() { return crosshairScreenY; }
        public boolean isDrawAxisLabels() { return drawAxisLabels; }
        public int getGridMinorDivisions() { return gridMinorDivisions; }
        public AxisXLocation getAxisXLocation() { return axisXLocation; }
        public AxisYLocation getAxisYLocation() { return axisYLocation; }
        public DoubleFunction<String> getXAxisLabelFormatter() { return xAxisLabelFormatter; }
        public DoubleFunction<AxisLabel> getXAxisLabelProvider() { return xAxisLabelProvider; }
        public Params setViewWidthPx(int viewWidthPx) { this.viewWidthPx = viewWidthPx; return this; }
        public Params setViewHeightPx(int viewHeightPx) { this.viewHeightPx = viewHeightPx; return this; }
        public Params setContentPadLeft(int contentPadLeft) { this.contentPadLeft = Math.max(0, contentPadLeft); return this; }
        public Params setContentPadTop(int contentPadTop) { this.contentPadTop = Math.max(0, contentPadTop); return this; }
        public Params setContentPadRight(int contentPadRight) { this.contentPadRight = Math.max(0, contentPadRight); return this; }
        public Params setContentPadBottom(int contentPadBottom) { this.contentPadBottom = Math.max(0, contentPadBottom); return this; }
        public Params setAxisLabelGapPx(int axisLabelGapPx) { this.axisLabelGapPx = Math.max(0, axisLabelGapPx); return this; }
        public Params setYAxisLabelWidthPx(int yAxisLabelWidthPx) { this.yAxisLabelWidthPx = Math.max(0, yAxisLabelWidthPx); return this; }
        public Params setXAxisLabelHeightPx(int xAxisLabelHeightPx) { this.xAxisLabelHeightPx = Math.max(0, xAxisLabelHeightPx); return this; }
        public Params setLabelTextSize(float labelTextSize) { this.labelTextSize = Math.max(8f, labelTextSize); return this; }
        public Params setDrawCrosshair(boolean drawCrosshair) { this.drawCrosshair = drawCrosshair; return this; }
        public Params setCrosshairScreenX(float crosshairScreenX) { this.crosshairScreenX = crosshairScreenX; return this; }
        public Params setCrosshairScreenY(float crosshairScreenY) { this.crosshairScreenY = crosshairScreenY; return this; }
        public Params setDrawAxisLabels(boolean drawAxisLabels) { this.drawAxisLabels = drawAxisLabels; return this; }
        public Params setGridMinorDivisions(int gridMinorDivisions) { this.gridMinorDivisions = Math.max(1, gridMinorDivisions); return this; }
        public Params setAxisXLocation(AxisXLocation axisXLocation) { this.axisXLocation = axisXLocation == null ? AxisXLocation.BOTTOM : axisXLocation; return this; }
        public Params setAxisYLocation(AxisYLocation axisYLocation) { this.axisYLocation = axisYLocation == null ? AxisYLocation.RIGHT : axisYLocation; return this; }
        public Params setXAxisLabelFormatter(DoubleFunction<String> xAxisLabelFormatter) { this.xAxisLabelFormatter = xAxisLabelFormatter; return this; }
        public Params setXAxisLabelProvider(DoubleFunction<AxisLabel> xAxisLabelProvider) { this.xAxisLabelProvider = xAxisLabelProvider; return this; }
    }

    public static final class ChartRect {
        private final int left;
        private final int top;
        private final int width;
        private final int height;

        public ChartRect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        public int left() { return left; }
        public int top() { return top; }
        public int width() { return width; }
        public int height() { return height; }
        public int right() { return left + width; }
        public int bottom() { return top + height; }
    }

    public ChartRect computeChartRect(Params p) {
        int left = p.getContentPadLeft() + reservedLeft(p);
        int top = p.getContentPadTop() + reservedTop(p);
        int right = Math.max(left + 1, p.getViewWidthPx() - p.getContentPadRight() - reservedRight(p));
        int bottom = Math.max(top + 1, p.getViewHeightPx() - p.getContentPadBottom() - reservedBottom(p));
        return new ChartRect(left, top, right - left, bottom - top);
    }

    public void buildBase(RenderList out, Viewport vp, Params p) {
        out.clear();
        if (vp == null) {
            return;
        }
        buildBaseIntoRect(out, vp, p, computeChartRect(p), true);
    }

    /**
     * Builds grid, axes, labels, and crosshair into a caller-supplied rectangle.
     */
    public void buildBaseIntoRect(RenderList out, Viewport vp, Params p, ChartRect rect, boolean clearFirst) {
        if (clearFirst) {
            out.clear();
        }
        if (vp == null || rect == null) {
            return;
        }
        Transform transform = new Transform(rect.left(), rect.top(), rect.width(), rect.height(), vp);
        addGrid(out, vp, transform, p.getGridMinorDivisions());
        addAxes(out, transform, p);
        if (p.isDrawAxisLabels()) {
            addAxisLabels(out, vp, transform, p.getLabelTextSize(), p.getGridMinorDivisions(), p);
        }
        if (p.isDrawCrosshair()) {
            addCrosshair(out, transform, p.getCrosshairScreenX(), p.getCrosshairScreenY(), p.getLabelTextSize(), p);
        }
    }

    private void addGrid(RenderList out, Viewport vp, Transform t, int div) {
        List<GridLines.Line> xs = GridLines.buildX(vp, t.width(), div);
        List<GridLines.Line> ys = GridLines.buildY(vp, t.height(), div);
        Style minor = new Style(ChartStyle.COLOR_GRID_MINOR, 1f, false);
        Style major = new Style(ChartStyle.COLOR_GRID_MAJOR, 1f, false);
        for (GridLines.Line line : xs) {
            int x = t.worldToScreenX(line.worldValue);
            if (x >= t.left() && x <= t.right()) {
                out.add(new RenderCommand.Line(x, t.top(), x, t.bottom(), line.major ? major : minor));
            }
        }
        for (GridLines.Line line : ys) {
            int y = t.worldToScreenY(line.worldValue);
            if (y >= t.top() && y <= t.bottom()) {
                out.add(new RenderCommand.Line(t.left(), y, t.right(), y, line.major ? major : minor));
            }
        }
    }

    private void addAxes(RenderList out, Transform t, Params p) {
        Style axis = new Style(ChartStyle.COLOR_AXIS, 1.3f, false);
        if (p.getAxisYLocation() == AxisYLocation.LEFT) {
            out.add(new RenderCommand.Line(t.left(), t.top(), t.left(), t.bottom(), axis));
        } else if (p.getAxisYLocation() == AxisYLocation.RIGHT) {
            out.add(new RenderCommand.Line(t.right(), t.top(), t.right(), t.bottom(), axis));
        }
        if (p.getAxisXLocation() == AxisXLocation.TOP) {
            out.add(new RenderCommand.Line(t.left(), t.top(), t.right(), t.top(), axis));
        } else if (p.getAxisXLocation() == AxisXLocation.BOTTOM) {
            out.add(new RenderCommand.Line(t.left(), t.bottom(), t.right(), t.bottom(), axis));
        }
    }

    private void addAxisLabels(RenderList out, Viewport vp, Transform t, float textSize, int div, Params p) {
        Style text = new Style(ChartStyle.COLOR_TEXT_MUTED, 1f, false);
        if (p.getAxisYLocation() != AxisYLocation.NONE) {
            int yLabelX = p.getAxisYLocation() == AxisYLocation.LEFT
                    ? Math.max(4, t.left() - p.getAxisLabelGapPx() - p.getYAxisLabelWidthPx())
                    : t.right() + p.getAxisLabelGapPx();
            for (GridLines.Line line : GridLines.buildY(vp, t.height(), div)) {
                if (!line.major) {
                    continue;
                }
                int y = t.worldToScreenY(line.worldValue);
                if (y >= t.top() && y <= t.bottom()) {
                    out.add(new RenderCommand.Text(yLabelX, y + (textSize * 0.35f), String.format(Locale.US, "%.2f", line.worldValue), textSize, text));
                }
            }
        }

        if (p.getAxisXLocation() != AxisXLocation.NONE) {
            float xLabelY = p.getAxisXLocation() == AxisXLocation.TOP
                    ? Math.max(10f, t.top() - p.getAxisLabelGapPx())
                    : t.bottom() + textSize + p.getAxisLabelGapPx();
            for (GridLines.Line line : GridLines.buildX(vp, t.width(), div)) {
                if (!line.major) {
                    continue;
                }
                int x = t.worldToScreenX(line.worldValue);
                if (x >= t.left() && x <= t.right()) {
                    AxisLabel axisLabel = p.getXAxisLabelProvider() == null
                            ? new AxisLabel(
                                    p.getXAxisLabelFormatter() == null
                                            ? String.valueOf((int) Math.round(line.worldValue))
                                            : p.getXAxisLabelFormatter().apply(line.worldValue),
                                    textSize,
                                    java.awt.Font.PLAIN)
                            : p.getXAxisLabelProvider().apply(line.worldValue);
                    String label = axisLabel == null || axisLabel.text() == null ? "" : axisLabel.text();
                    float labelTextSize = axisLabel == null ? textSize : axisLabel.textSize();
                    int fontStyle = axisLabel == null ? java.awt.Font.PLAIN : axisLabel.fontStyle();
                    out.add(new RenderCommand.Text(x - 20, xLabelY, label, labelTextSize, fontStyle, text));
                }
            }
        }
    }

    private void addCrosshair(RenderList out, Transform t, float x, float y, float textSize, Params p) {
        if (x < t.left() || x > t.right() || y < t.top() || y > t.bottom()) {
            return;
        }
        Style cross = new Style(ChartStyle.COLOR_CROSS, 1f, false);
        Style text = new Style(ChartStyle.COLOR_TEXT, 1f, false);
        out.add(new RenderCommand.Line(x, t.top(), x, t.bottom(), cross));
        out.add(new RenderCommand.Line(t.left(), y, t.right(), y, cross));

        if (p.getAxisYLocation() != AxisYLocation.NONE) {
            float left = p.getAxisYLocation() == AxisYLocation.LEFT
                    ? Math.max(4f, t.left() - p.getYAxisLabelWidthPx())
                    : t.right() + 4f;
            out.add(new RenderCommand.RoundedRect(left, y - 14f, left + 54f, y + 6f, 7f, new Style(ChartStyle.COLOR_PANEL, 1f, true).withCornerRadius(7f)));
            out.add(new RenderCommand.Text(left + 8f, y, String.format(Locale.US, "%.2f", t.screenToWorldY(y)), textSize, text));
        }
    }

    private int reservedLeft(Params p) {
        return p.getAxisYLocation() == AxisYLocation.LEFT ? p.getYAxisLabelWidthPx() + p.getAxisLabelGapPx() : 0;
    }

    private int reservedRight(Params p) {
        return p.getAxisYLocation() == AxisYLocation.RIGHT ? p.getYAxisLabelWidthPx() + p.getAxisLabelGapPx() : 0;
    }

    private int reservedTop(Params p) {
        return p.getAxisXLocation() == AxisXLocation.TOP ? p.getXAxisLabelHeightPx() + p.getAxisLabelGapPx() : 0;
    }

    private int reservedBottom(Params p) {
        return p.getAxisXLocation() == AxisXLocation.BOTTOM ? p.getXAxisLabelHeightPx() + p.getAxisLabelGapPx() : 0;
    }
}

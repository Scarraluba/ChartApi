package concrete.goonie.chart.internal.render;

import concrete.goonie.chart.api.model.Viewport;

/** Internal world/screen transform. */
public final class Transform {
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final Viewport viewport;

    public Transform(int left, int top, int width, int height, Viewport viewport) {
        this.left = left;
        this.top = top;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.viewport = viewport;
        viewport.ensureYNonZero();
    }

    public int worldToScreenX(double x) {
        double t = (x - viewport.getXMin()) / (viewport.getXMax() - viewport.getXMin());
        return (int) Math.round(left + t * width);
    }

    public int worldToScreenY(double y) {
        double t = (y - viewport.getYMin()) / (viewport.getYMax() - viewport.getYMin());
        return (int) Math.round(top + (1.0 - t) * height);
    }

    public double screenToWorldX(double x) {
        double t = (x - left) / (double) width;
        return viewport.getXMin() + t * (viewport.getXMax() - viewport.getXMin());
    }

    public double screenToWorldY(double y) {
        double t = 1.0 - ((y - top) / (double) height);
        return viewport.getYMin() + t * (viewport.getYMax() - viewport.getYMin());
    }

    public double pixelsPerWorldX() {
        return width / (viewport.getXMax() - viewport.getXMin());
    }

    public double pixelsPerWorldY() {
        return height / (viewport.getYMax() - viewport.getYMin());
    }

    public int left() { return left; }
    public int top() { return top; }
    public int right() { return left + width; }
    public int bottom() { return top + height; }
    public int width() { return width; }
    public int height() { return height; }
}

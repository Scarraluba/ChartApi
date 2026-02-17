package concrete.goonie;

public final class Transform {
    private final int left, top, width, height;
    private final Viewport vp;

    public Transform(int left, int top, int width, int height, Viewport vp) {
        this.left = left;
        this.top = top;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.vp = vp;
        vp.ensureYNonZero();
    }

    public int worldToScreenX(double x) {
        double t = (x - vp.xMin) / (vp.xMax - vp.xMin);
        return (int) Math.round(left + t * width);
    }

    public int worldToScreenY(double y) {
        double t = (y - vp.yMin) / (vp.yMax - vp.yMin);
        return (int) Math.round(top + (1.0 - t) * height);
    }

    public double screenToWorldX(double sx) {
        double t = (sx - left) / (double) width;
        return vp.xMin + t * (vp.xMax - vp.xMin);
    }

    public double screenToWorldY(double sy) {
        double t = 1.0 - ((sy - top) / (double) height);
        return vp.yMin + t * (vp.yMax - vp.yMin);
    }

    public double pixelsPerWorldX() { return width / (vp.xMax - vp.xMin); }
    public double pixelsPerWorldY() { return height / (vp.yMax - vp.yMin); }

    public int left() { return left; }
    public int top() { return top; }
    public int right() { return left + width; }
    public int bottom() { return top + height; }
    public int width() { return width; }
    public int height() { return height; }
}
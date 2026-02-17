package concrete.goonie.chart.api.model;

import concrete.goonie.chart.api.render.AxisRangePolicy;

/**
 * Mutable chart viewport.
 */
public final class Viewport {
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;

    public Viewport(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        ensureYNonZero();
    }

    public double getXMin() { return xMin; }
    public double getXMax() { return xMax; }
    public double getYMin() { return yMin; }
    public double getYMax() { return yMax; }

    public void setX(double xMin, double xMax) {
        this.xMin = xMin;
        this.xMax = xMax;
    }

    public void setY(double yMin, double yMax) {
        this.yMin = yMin;
        this.yMax = yMax;
        ensureYNonZero();
    }

    public void setY(double yMin, double yMax, AxisRangePolicy policy) {
        double[] applied = policy == null ? new double[]{yMin, yMax} : policy.apply(yMin, yMax);
        this.yMin = applied[0];
        this.yMax = applied[1];
        ensureYNonZero();
    }

    public void setX(double xMin, double xMax, AxisRangePolicy policy) {
        double[] applied = policy == null ? new double[]{xMin, xMax} : policy.apply(xMin, xMax);
        this.xMin = applied[0];
        this.xMax = applied[1];
    }

    public void translate(double dx, double dy) {
        xMin += dx;
        xMax += dx;
        yMin += dy;
        yMax += dy;
    }

    public void clampX(double min, double max) {
        double range = xRange();
        if (range <= 0.0) range = 1.0;
        if (xMin < min) {
            xMin = min;
            xMax = min + range;
        }
        if (xMax > max) {
            xMax = max;
            xMin = max - range;
        }
    }

    public void applyYPolicy(AxisRangePolicy policy) {
        if (policy == null) {
            ensureYNonZero();
            return;
        }
        double[] applied = policy.apply(yMin, yMax);
        yMin = applied[0];
        yMax = applied[1];
        ensureYNonZero();
    }

    public void applyXPolicy(AxisRangePolicy policy) {
        if (policy == null) {
            return;
        }
        double[] applied = policy.apply(xMin, xMax);
        xMin = applied[0];
        xMax = applied[1];
    }

    public void ensureYNonZero() {
        if (yRange() <= 1e-9) {
            double mid = (yMin + yMax) * 0.5;
            yMin = mid - 1.0;
            yMax = mid + 1.0;
        }
    }

    public double xRange() { return xMax - xMin; }
    public double yRange() { return yMax - yMin; }
}

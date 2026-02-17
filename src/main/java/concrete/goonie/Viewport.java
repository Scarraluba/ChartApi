package concrete.goonie;

public final class Viewport {
    // World X is candle index in double (smooth pan)
    public double xMin;
    public double xMax;

    // World Y is price
    public double yMin;
    public double yMax;

    public Viewport(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public double xRange() { return xMax - xMin; }
    public double yRange() { return yMax - yMin; }

    public void ensureYNonZero() {
        if (yRange() <= 1e-9) {
            double mid = (yMin + yMax) * 0.5;
            yMin = mid - 1;
            yMax = mid + 1;
        }
    }

    public void clampX(double min, double max) {
        double r = xRange();
        if (r <= 0) r = 1;

        if (xMin < min) { xMin = min; xMax = min + r; }
        if (xMax > max) { xMax = max; xMin = max - r; }
    }
}
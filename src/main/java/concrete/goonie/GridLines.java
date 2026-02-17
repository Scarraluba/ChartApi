package concrete.goonie;

import java.util.ArrayList;
import java.util.List;

public final class GridLines {

    public static final class Line {
        public final double worldValue; // x index or y price
        public final boolean major;

        public Line(double worldValue, boolean major) {
            this.worldValue = worldValue;
            this.major = major;
        }
    }

    public static List<Line> buildY(Viewport vp, int pixelHeight, int minorDivisions) {
        int targetMajor = clamp(pixelHeight / 40, 5, 10);
        double majorStep = NiceScale.niceStep(vp.yRange(), targetMajor);
        double minorStep = majorStep / minorDivisions;

        double start = NiceScale.firstLine(vp.yMin, minorStep);
        double end = NiceScale.lastLine(vp.yMax, minorStep);

        List<Line> out = new ArrayList<>();
        for (double y = start; y <= end + 1e-12; y += minorStep) {
            boolean major = isMultipleOf(y, majorStep, 1e-10);
            out.add(new Line(y, major));
        }
        return out;
    }

    public static List<Line> buildX(Viewport vp, int pixelWidth, int minorDivisions) {
        int targetMajor = clamp(pixelWidth / 60, 6, 14);
        double majorStep = NiceScale.niceStep(vp.xRange(), targetMajor);
        double minorStep = majorStep / minorDivisions;

        double start = NiceScale.firstLine(vp.xMin, minorStep);
        double end = NiceScale.lastLine(vp.xMax, minorStep);

        List<Line> out = new ArrayList<>();
        for (double x = start; x <= end + 1e-12; x += minorStep) {
            boolean major = isMultipleOf(x, majorStep, 1e-10);
            out.add(new Line(x, major));
        }
        return out;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean isMultipleOf(double value, double step, double eps) {
        if (step == 0) return false;
        double k = value / step;
        return Math.abs(k - Math.rint(k)) < eps;
    }
}
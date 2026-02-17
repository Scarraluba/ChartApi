package concrete.goonie.chart.internal.render;

import concrete.goonie.chart.api.model.Viewport;

import java.util.ArrayList;
import java.util.List;

/** Internal grid generation. */
public final class GridLines {
    private GridLines() {}

    public static final class Line {
        public final double worldValue;
        public final boolean major;

        public Line(double worldValue, boolean major) {
            this.worldValue = worldValue;
            this.major = major;
        }
    }

    public static List<Line> buildY(Viewport viewport, int pixelHeight, int minorDivisions) {
        int targetMajor = clamp(pixelHeight / 40, 5, 10);
        double majorStep = NiceScale.niceStep(viewport.yRange(), targetMajor);
        double minorStep = majorStep / Math.max(1, minorDivisions);
        double start = NiceScale.firstLine(viewport.getYMin(), minorStep);
        double end = NiceScale.lastLine(viewport.getYMax(), minorStep);

        List<Line> out = new ArrayList<>();
        for (double y = start; y <= end + 1e-12; y += minorStep) {
            out.add(new Line(y, isMultipleOf(y, majorStep, 1e-10)));
        }
        return out;
    }

    public static List<Line> buildX(Viewport viewport, int pixelWidth, int minorDivisions) {
        int targetMajor = clamp(pixelWidth / 60, 6, 14);
        double majorStep = NiceScale.niceStep(viewport.xRange(), targetMajor);
        double minorStep = majorStep / Math.max(1, minorDivisions);
        double start = NiceScale.firstLine(viewport.getXMin(), minorStep);
        double end = NiceScale.lastLine(viewport.getXMax(), minorStep);

        List<Line> out = new ArrayList<>();
        for (double x = start; x <= end + 1e-12; x += minorStep) {
            out.add(new Line(x, isMultipleOf(x, majorStep, 1e-10)));
        }
        return out;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isMultipleOf(double value, double step, double eps) {
        if (step == 0) return false;
        double k = value / step;
        return Math.abs(k - Math.rint(k)) < eps;
    }
}

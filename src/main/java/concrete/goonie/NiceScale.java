package concrete.goonie;

public final class NiceScale {

    public static double niceStep(double range, int targetTicks) {
        if (range <= 0) return 1;
        if (targetTicks < 2) targetTicks = 2;

        double raw = range / (targetTicks - 1.0);
        double exp = Math.floor(Math.log10(raw));
        double base = Math.pow(10, exp);
        double f = raw / base;

        double nice;
        if (f <= 1.0) nice = 1.0;
        else if (f <= 2.0) nice = 2.0;
        else if (f <= 2.5) nice = 2.5;
        else if (f <= 5.0) nice = 5.0;
        else nice = 10.0;

        return nice * base;
    }

    public static double firstLine(double min, double step) {
        return Math.floor(min / step) * step;
    }

    public static double lastLine(double max, double step) {
        return Math.ceil(max / step) * step;
    }
}
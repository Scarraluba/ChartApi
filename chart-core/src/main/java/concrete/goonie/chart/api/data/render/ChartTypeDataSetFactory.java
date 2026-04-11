package concrete.goonie.chart.api.data.render;

import concrete.goonie.chart.api.data.BarDataSet;
import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.data.ChartType;
import concrete.goonie.chart.api.data.CompositeDataSet;
import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.data.LineDataSet;
import concrete.goonie.chart.api.data.RectDataSet;
import concrete.goonie.chart.api.data.TextDataSet;
import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Produces a renderable dataset for the requested core chart type.
 * Every chart type is derived from the same source candle stream so the X alignment remains shared.
 */
public final class ChartTypeDataSetFactory {
    private ChartTypeDataSetFactory() {}

    public static DataSet create(CandlestickDataSet mainDataSet, ChartType chartType) {
        if (mainDataSet == null) {
            return null;
        }
        ChartType safeType = chartType == null ? ChartType.CANDLES : chartType;
        return switch (safeType) {
            case CANDLES -> cloneCandles(mainDataSet, "Candles");
            case HOLLOW_CANDLES -> buildHollowCandles(mainDataSet);
            case VOLUME_CANDLES -> buildVolumeCandles(mainDataSet);
            case LINE -> buildLine(mainDataSet, "Line", SeriesMode.CLOSE, 0xFF60A5FA, 2.2f);
            case LINE_WITH_MARKERS -> buildLineWithMarkers(mainDataSet);
            case STEP_LINE -> buildStepLine(mainDataSet);
            case AREA -> buildArea(mainDataSet, false);
            case HLC_AREA -> buildArea(mainDataSet, true);
            case BASELINE -> buildBaseline(mainDataSet);
            case COLUMNS -> buildColumns(mainDataSet);
            case HIGH_LOW -> buildHighLow(mainDataSet);
            case VOLUME_FOOTPRINT -> buildVolumeFootprint(mainDataSet);
            case TIME_PRICE_OPPORTUNITY -> buildTimePriceOpportunity(mainDataSet);
            case SESSION_VOLUME_PROFILE -> buildSessionVolumeProfile(mainDataSet);
            case HEIKIN_ASHI -> buildHeikinAshi(mainDataSet);
            case RENKO -> buildRenko(mainDataSet);
            case LINE_BREAK -> buildLineBreak(mainDataSet);
            case KAGI -> buildKagi(mainDataSet);
            case POINT_AND_FIGURE -> buildPointAndFigure(mainDataSet);
            case RANGE -> buildRange(mainDataSet);
        };
    }

    private static CandlestickDataSet cloneCandles(CandlestickDataSet source, String name) {
        CandlestickDataSet copy = new CandlestickDataSet(name, source.getSeries(), copyTimeframes(source));
        copy.setWickStyle(copyStyle(source.getWickStyle()));
        copy.setBullBodyStyle(copyStyle(source.getBullBodyStyle()));
        copy.setBearBodyStyle(copyStyle(source.getBearBodyStyle()));
        copy.setOptimizationOptions(source.getOptimizationOptions());
        return copy;
    }

    private static DataSet buildHollowCandles(CandlestickDataSet source) {
        CandlestickDataSet candles = cloneCandles(source, "Hollow candles");
        Style bull = copyStyle(candles.getBullBodyStyle());
        bull.fill = false;
        bull.strokeWidth = 1.6f;
        Style bear = copyStyle(candles.getBearBodyStyle());
        bear.fill = true;
        bear.strokeWidth = 1.3f;
        candles.setBullBodyStyle(bull);
        candles.setBearBodyStyle(bear);
        return candles;
    }

    private static DataSet buildVolumeCandles(CandlestickDataSet source) {
        CandlestickDataSet candles = cloneCandles(source, "Volume candles");
        CompositeDataSet composite = new CompositeDataSet("Volume candles", copyTimeframes(source)).add(candles);
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return composite;
        }
        double min = series.minLow(0, size - 1);
        double max = series.maxHigh(0, size - 1);
        double range = Math.max(1e-6, max - min);
        long maxVolume = 1L;
        for (int i = 0; i < size; i++) {
            maxVolume = Math.max(maxVolume, series.get(i).volume());
        }
        double[] x = new double[size];
        double[] y = new double[size];
        double base = min + range * 0.02;
        for (int i = 0; i < size; i++) {
            x[i] = i;
            double normalized = series.get(i).volume() / (double) maxVolume;
            y[i] = base + normalized * range * 0.16;
        }
        BarDataSet volume = new BarDataSet("Volume intensity", x, y, base, copyTimeframes(source));
        volume.setBarStyle(new Style(0x5538BDF8, 1f, true));
        volume.setOptimizationOptions(source.getOptimizationOptions());
        composite.add(volume);
        return composite;
    }

    private static DataSet buildLineWithMarkers(CandlestickDataSet source) {
        CompositeDataSet composite = new CompositeDataSet("Line with markers", copyTimeframes(source));
        composite.add(buildLine(source, "Line", SeriesMode.CLOSE, 0xFF60A5FA, 2.1f));
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        double[] x = new double[size];
        double[] y = new double[size];
        double[] x2 = new double[size];
        double[] y2 = new double[size];
        for (int i = 0; i < size; i++) {
            double px = i;
            double py = series.get(i).close();
            x[i] = px - 0.10;
            x2[i] = px + 0.10;
            y[i] = py - 0.10;
            y2[i] = py + 0.10;
        }
        RectDataSet markers = new RectDataSet("Markers", x, y, x2, y2, null, copyTimeframes(source));
        markers.setRectStyle(new Style(0xFFDBEAFE, 1f, true).withCornerRadius(4f));
        markers.setBorderStyle(new Style(0xFF1D4ED8, 1f, false).withCornerRadius(4f));
        composite.add(markers);
        return composite;
    }

    private static DataSet buildStepLine(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return buildLine(source, "Step line", SeriesMode.CLOSE, 0xFFF59E0B, 2f);
        }
        double[] x = new double[Math.max(1, size * 2 - 1)];
        double[] y = new double[x.length];
        int cursor = 0;
        x[cursor] = 0;
        y[cursor] = series.get(0).close();
        cursor++;
        for (int i = 1; i < size; i++) {
            x[cursor] = i;
            y[cursor] = series.get(i - 1).close();
            cursor++;
            x[cursor] = i;
            y[cursor] = series.get(i).close();
            cursor++;
        }
        LineDataSet line = new LineDataSet("Step line", x, y, copyTimeframes(source));
        line.setLineStyle(new Style(0xFFF59E0B, 2f, false));
        line.setOptimizationOptions(source.getOptimizationOptions());
        return line;
    }

    private static DataSet buildArea(CandlestickDataSet source, boolean hlc) {
        CompositeDataSet composite = new CompositeDataSet(hlc ? "HLC area" : "Area", copyTimeframes(source));
        LineDataSet line = (LineDataSet) buildLine(source, hlc ? "HLC area line" : "Area line", hlc ? SeriesMode.HLC3 : SeriesMode.CLOSE,
                hlc ? 0xFF22C55E : 0xFF38BDF8, 2f);
        composite.add(line);
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size > 0) {
            double[] x = new double[size];
            double[] y = new double[size];
            double min = series.minLow(0, size - 1);
            double max = series.maxHigh(0, size - 1);
            double base = min - (max - min) * 0.02;
            for (int i = 0; i < size; i++) {
                x[i] = i;
                y[i] = valueFor(series.get(i), hlc ? SeriesMode.HLC3 : SeriesMode.CLOSE);
            }
            BarDataSet fill = new BarDataSet(hlc ? "HLC fill" : "Area fill", x, y, base, copyTimeframes(source));
            fill.setBarStyle(new Style(hlc ? 0x3322C55E : 0x3338BDF8, 1f, true));
            fill.setOptimizationOptions(source.getOptimizationOptions());
            composite.add(fill);
        }
        return composite;
    }

    private static DataSet buildBaseline(CandlestickDataSet source) {
        CompositeDataSet composite = new CompositeDataSet("Baseline", copyTimeframes(source));
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return composite;
        }
        double[] x = new double[size];
        double[] y = new double[size];
        double baseline = series.get(0).close();
        for (int i = 0; i < size; i++) {
            x[i] = i;
            y[i] = series.get(i).close();
        }
        BarDataSet bars = new BarDataSet("Baseline bars", x, y, baseline, copyTimeframes(source));
        bars.setBarStyle(new Style(0x228A8A8A, 1f, true));
        bars.setOptimizationOptions(source.getOptimizationOptions());
        composite.add(bars);
        LineDataSet line = new LineDataSet("Baseline line", x, y, copyTimeframes(source));
        line.setLineStyle(new Style(0xFFEAB308, 2.2f, false));
        line.setOptimizationOptions(source.getOptimizationOptions());
        composite.add(line);
        return composite;
    }

    private static DataSet buildColumns(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        double[] x = new double[size];
        double[] y = new double[size];
        double base = size == 0 ? 0.0 : series.minLow(0, size - 1) - Math.max(1e-6, (series.maxHigh(0, size - 1) - series.minLow(0, size - 1)) * 0.02);
        for (int i = 0; i < size; i++) {
            x[i] = i;
            y[i] = series.get(i).close();
        }
        BarDataSet bars = new BarDataSet("Columns", x, y, base, copyTimeframes(source));
        bars.setBarStyle(new Style(0xCC60A5FA, 1f, true));
        bars.setOptimizationOptions(source.getOptimizationOptions());
        return bars;
    }

    private static DataSet buildHighLow(CandlestickDataSet source) {
        CandlestickDataSet candles = cloneCandles(source, "High-low");
        Style wick = copyStyle(candles.getWickStyle());
        wick.strokeWidth = 1.8f;
        wick.argb = 0xFFE2E8F0;
        candles.setWickStyle(wick);
        Style body = new Style(0x00000000, 1f, false);
        candles.setBullBodyStyle(body.copy());
        candles.setBearBodyStyle(body.copy());
        return candles;
    }

    private static DataSet buildVolumeFootprint(CandlestickDataSet source) {
        CompositeDataSet composite = new CompositeDataSet("Volume footprint", copyTimeframes(source));
        composite.add(buildHollowCandles(source));
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return composite;
        }
        int stride = Math.max(1, size / 50);
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<String> t = new ArrayList<>();
        for (int i = 0; i < size; i += stride) {
            Candle candle = series.get(i);
            x.add((double) i);
            y.add((candle.high() + candle.low()) * 0.5);
            t.add(Long.toString(candle.volume()));
        }
        TextDataSet text = new TextDataSet("Volume labels", toArray(x), toArray(y), t.toArray(String[]::new), copyTimeframes(source));
        text.setTextStyle(new Style(0x88F8FAFC, 1f, false));
        composite.add(text);
        return composite;
    }

    private static DataSet buildTimePriceOpportunity(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return new CompositeDataSet("TPO", copyTimeframes(source));
        }
        int bucketCount = Math.max(8, Math.min(24, (int) Math.sqrt(size)));
        double min = series.minLow(0, size - 1);
        double max = series.maxHigh(0, size - 1);
        double step = Math.max(1e-6, (max - min) / bucketCount);
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<String> text = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Candle candle = series.get(i);
            int bucket = (int) Math.floor((candle.close() - min) / step);
            bucket = Math.max(0, Math.min(bucketCount - 1, bucket));
            x.add((double) i);
            y.add(min + bucket * step + step * 0.5);
            text.add(String.valueOf((char) ('A' + (i % 26))));
        }
        TextDataSet tpo = new TextDataSet("TPO", toArray(x), toArray(y), text.toArray(String[]::new), copyTimeframes(source));
        tpo.setTextStyle(new Style(0x99A7F3D0, 1f, false));
        return tpo;
    }

    private static DataSet buildSessionVolumeProfile(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return new CompositeDataSet("Session volume profile", copyTimeframes(source));
        }
        int bucketCount = Math.max(10, Math.min(32, (int) Math.sqrt(size) + 6));
        double min = series.minLow(0, size - 1);
        double max = series.maxHigh(0, size - 1);
        double step = Math.max(1e-6, (max - min) / bucketCount);
        double[] volumes = new double[bucketCount];
        for (int i = 0; i < size; i++) {
            Candle candle = series.get(i);
            int bucket = (int) Math.floor(((candle.high() + candle.low()) * 0.5 - min) / step);
            bucket = Math.max(0, Math.min(bucketCount - 1, bucket));
            volumes[bucket] += candle.volume();
        }
        double maxVol = 1.0;
        for (double v : volumes) {
            maxVol = Math.max(maxVol, v);
        }
        double rightX = Math.max(1, size - 1);
        double[] x = new double[bucketCount];
        double[] y = new double[bucketCount];
        double[] x2 = new double[bucketCount];
        double[] y2 = new double[bucketCount];
        String[] labels = new String[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            double normalized = volumes[i] / maxVol;
            x[i] = rightX - normalized * Math.max(6.0, size * 0.18);
            x2[i] = rightX;
            y[i] = min + i * step;
            y2[i] = y[i] + step * 0.86;
            labels[i] = String.format(Locale.US, "%.0f", volumes[i]);
        }
        RectDataSet profile = new RectDataSet("Session volume profile", x, y, x2, y2, labels, copyTimeframes(source));
        profile.setRectStyle(new Style(0x5538BDF8, 1f, true).withCornerRadius(3f));
        profile.setBorderStyle(new Style(0x8860A5FA, 1f, false).withCornerRadius(3f));
        return profile;
    }

    private static DataSet buildHeikinAshi(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        CandleSeries transformed = new CandleSeries();
        if (series != null && !series.isEmpty()) {
            double prevOpen = (series.get(0).open() + series.get(0).close()) * 0.5;
            double prevClose = (series.get(0).open() + series.get(0).high() + series.get(0).low() + series.get(0).close()) * 0.25;
            transformed.add(createCandle(series.get(0).time(), prevOpen, Math.max(series.get(0).high(), Math.max(prevOpen, prevClose)), Math.min(series.get(0).low(), Math.min(prevOpen, prevClose)), prevClose, series.get(0).volume(), series.get(0).spread()));
            for (int i = 1; i < series.size(); i++) {
                Candle candle = series.get(i);
                double haClose = (candle.open() + candle.high() + candle.low() + candle.close()) * 0.25;
                double haOpen = (prevOpen + prevClose) * 0.5;
                double haHigh = Math.max(candle.high(), Math.max(haOpen, haClose));
                double haLow = Math.min(candle.low(), Math.min(haOpen, haClose));
                transformed.add(createCandle(candle.time(), haOpen, haHigh, haLow, haClose, candle.volume(), candle.spread()));
                prevOpen = haOpen;
                prevClose = haClose;
            }
        }
        CandlestickDataSet dataSet = new CandlestickDataSet("Heikin Ashi", transformed, copyTimeframes(source));
        dataSet.setOptimizationOptions(source.getOptimizationOptions());
        dataSet.setWickStyle(new Style(0x99F8FAFC, 1.2f, false));
        dataSet.setBullBodyStyle(new Style(0xCC22C55E, 1f, true));
        dataSet.setBearBodyStyle(new Style(0xCCE11D48, 1f, true));
        return dataSet;
    }

    private static DataSet buildRenko(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        CandleSeries transformed = new CandleSeries();
        if (series != null && !series.isEmpty()) {
            double box = averageRange(series) * 0.8;
            box = Math.max(box, 0.0001);
            double anchor = series.get(0).close();
            for (int i = 0; i < series.size(); i++) {
                Candle candle = series.get(i);
                double target = anchor;
                while (candle.close() >= target + box) {
                    target += box;
                }
                while (candle.close() <= target - box) {
                    target -= box;
                }
                double open = anchor;
                double close = target;
                double high = Math.max(open, close);
                double low = Math.min(open, close);
                transformed.add(createCandle(candle.time(), open, high, low, close, candle.volume(), candle.spread()));
                anchor = close;
            }
        }
        CandlestickDataSet dataSet = new CandlestickDataSet("Renko", transformed, copyTimeframes(source));
        dataSet.setOptimizationOptions(source.getOptimizationOptions());
        return dataSet;
    }

    private static DataSet buildLineBreak(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        CandleSeries transformed = new CandleSeries();
        if (series != null && !series.isEmpty()) {
            List<Double> closes = new ArrayList<>();
            double prev = series.get(0).close();
            closes.add(prev);
            transformed.add(createCandle(series.get(0).time(), prev, prev, prev, prev, series.get(0).volume(), series.get(0).spread()));
            for (int i = 1; i < series.size(); i++) {
                Candle candle = series.get(i);
                double highest = closes.stream().skip(Math.max(0, closes.size() - 3)).mapToDouble(Double::doubleValue).max().orElse(prev);
                double lowest = closes.stream().skip(Math.max(0, closes.size() - 3)).mapToDouble(Double::doubleValue).min().orElse(prev);
                double next = prev;
                if (candle.close() > highest) {
                    next = candle.close();
                } else if (candle.close() < lowest) {
                    next = candle.close();
                }
                transformed.add(createCandle(candle.time(), prev, Math.max(prev, next), Math.min(prev, next), next, candle.volume(), candle.spread()));
                closes.add(next);
                prev = next;
            }
        }
        CandlestickDataSet dataSet = new CandlestickDataSet("Line break", transformed, copyTimeframes(source));
        dataSet.setOptimizationOptions(source.getOptimizationOptions());
        dataSet.setBullBodyStyle(new Style(0xCC34D399, 1f, true));
        dataSet.setBearBodyStyle(new Style(0xCCF43F5E, 1f, true));
        return dataSet;
    }

    private static DataSet buildKagi(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return buildLine(source, "Kagi", SeriesMode.CLOSE, 0xFFA78BFA, 2.4f);
        }
        double reversal = Math.max(averageRange(series) * 0.75, 0.0001);
        double[] x = new double[Math.max(1, size * 2 - 1)];
        double[] y = new double[x.length];
        double current = series.get(0).close();
        int cursor = 0;
        x[cursor] = 0;
        y[cursor] = current;
        cursor++;
        for (int i = 1; i < size; i++) {
            double close = series.get(i).close();
            double next = Math.abs(close - current) >= reversal ? close : current;
            x[cursor] = i;
            y[cursor] = current;
            cursor++;
            x[cursor] = i;
            y[cursor] = next;
            cursor++;
            current = next;
        }
        LineDataSet line = new LineDataSet("Kagi", trim(x, cursor), trim(y, cursor), copyTimeframes(source));
        line.setLineStyle(new Style(0xFFA78BFA, 2.6f, false));
        line.setOptimizationOptions(source.getOptimizationOptions());
        return line;
    }

    private static DataSet buildPointAndFigure(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        int size = series == null ? 0 : series.size();
        if (size == 0) {
            return new CompositeDataSet("Point & figure", copyTimeframes(source));
        }
        double box = Math.max(averageRange(series) * 0.9, 0.0001);
        double[] x = new double[size];
        double[] y = new double[size];
        String[] text = new String[size];
        double anchor = series.get(0).close();
        for (int i = 0; i < size; i++) {
            Candle candle = series.get(i);
            x[i] = i;
            y[i] = anchor;
            if (candle.close() >= anchor + box) {
                anchor += box;
                text[i] = "X";
            } else if (candle.close() <= anchor - box) {
                anchor -= box;
                text[i] = "O";
            } else {
                text[i] = (i & 1) == 0 ? "·" : "";
            }
            y[i] = anchor;
        }
        TextDataSet dataSet = new TextDataSet("Point & figure", x, y, text, copyTimeframes(source));
        dataSet.setTextStyle(new Style(0xFFCBD5E1, 1f, false));
        return dataSet;
    }

    private static DataSet buildRange(CandlestickDataSet source) {
        CandleSeries series = source.getSeries();
        CandleSeries transformed = new CandleSeries();
        if (series != null && !series.isEmpty()) {
            double box = Math.max(averageRange(series), 0.0001);
            double open = series.get(0).open();
            for (int i = 0; i < series.size(); i++) {
                Candle candle = series.get(i);
                double close = open;
                if (candle.close() >= open + box) {
                    close = open + box;
                } else if (candle.close() <= open - box) {
                    close = open - box;
                }
                double high = Math.max(open, close);
                double low = Math.min(open, close);
                transformed.add(createCandle(candle.time(), open, high, low, close, candle.volume(), candle.spread()));
                open = close;
            }
        }
        CandlestickDataSet dataSet = new CandlestickDataSet("Range", transformed, copyTimeframes(source));
        dataSet.setOptimizationOptions(source.getOptimizationOptions());
        dataSet.setBullBodyStyle(new Style(0xCC10B981, 1f, true));
        dataSet.setBearBodyStyle(new Style(0xCCEF4444, 1f, true));
        return dataSet;
    }

    private static LineDataSet buildLine(CandlestickDataSet mainDataSet, String name, SeriesMode mode, int color, float strokeWidth) {
        CandleSeries series = mainDataSet.getSeries();
        int size = series == null ? 0 : series.size();
        double[] x = new double[size];
        double[] y = new double[size];
        for (int i = 0; i < size; i++) {
            x[i] = i;
            y[i] = valueFor(series.get(i), mode);
        }
        LineDataSet lineDataSet = new LineDataSet(name, x, y, copyTimeframes(mainDataSet));
        lineDataSet.setLineStyle(new Style(color, strokeWidth, false));
        lineDataSet.setOptimizationOptions(mainDataSet.getOptimizationOptions());
        return lineDataSet;
    }

    private static Style copyStyle(Style style) {
        return style == null ? new Style() : style.copy();
    }

    private static Timeframe[] copyTimeframes(DataSet dataSet) {
        return dataSet.getSelectedTimeframes().toArray(Timeframe[]::new);
    }

    private static Candle createCandle(java.time.LocalDateTime time, double open, double high, double low, double close, long volume, long spread) {
        double h = Math.max(high, Math.max(open, close));
        double l = Math.min(low, Math.min(open, close));
        return new Candle(time, open, h, l, close, volume, spread);
    }

    private static double averageRange(CandleSeries series) {
        if (series == null || series.isEmpty()) {
            return 1.0;
        }
        double total = 0.0;
        for (int i = 0; i < series.size(); i++) {
            Candle candle = series.get(i);
            total += Math.max(1e-6, candle.high() - candle.low());
        }
        return total / Math.max(1, series.size());
    }

    private static double valueFor(Candle candle, SeriesMode mode) {
        return switch (mode) {
            case OPEN -> candle.open();
            case HIGH -> candle.high();
            case LOW -> candle.low();
            case CLOSE -> candle.close();
            case HLC3 -> (candle.high() + candle.low() + candle.close()) / 3.0;
        };
    }

    private static double[] toArray(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static double[] trim(double[] values, int size) {
        double[] trimmed = new double[Math.max(0, size)];
        System.arraycopy(values, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private enum SeriesMode {
        OPEN,
        HIGH,
        LOW,
        CLOSE,
        HLC3
    }
}

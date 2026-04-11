package concrete.goonie.chart.api.data;

/**
 * Core chart type contract shared across Swing, Android, and JavaFX hosts.
 * The type defines the preferred render family for the main price series.
 */
public enum ChartType {
    CANDLES("Candles", false, DataSetType.CANDLESTICK),
    HOLLOW_CANDLES("Hollow candles", false, DataSetType.CANDLESTICK),
    VOLUME_CANDLES("Volume candles", false, DataSetType.CANDLESTICK),
    LINE("Line", true, DataSetType.LINE),
    LINE_WITH_MARKERS("Line with markers", true, DataSetType.LINE),
    STEP_LINE("Step line", true, DataSetType.LINE),
    AREA("Area", true, DataSetType.LINE),
    HLC_AREA("HLC area", true, DataSetType.LINE),
    BASELINE("Baseline", true, DataSetType.LINE),
    COLUMNS("Columns", false, DataSetType.BAR),
    HIGH_LOW("High-low", false, DataSetType.CANDLESTICK),
    VOLUME_FOOTPRINT("Volume footprint", false, DataSetType.CANDLESTICK),
    TIME_PRICE_OPPORTUNITY("Time price opportunity", false, DataSetType.CANDLESTICK),
    SESSION_VOLUME_PROFILE("Session volume profile", false, DataSetType.CANDLESTICK),
    HEIKIN_ASHI("Heikin Ashi", false, DataSetType.CANDLESTICK),
    RENKO("Renko", false, DataSetType.CANDLESTICK),
    LINE_BREAK("Line break", false, DataSetType.CANDLESTICK),
    KAGI("Kagi", false, DataSetType.CANDLESTICK),
    POINT_AND_FIGURE("Point & figure", false, DataSetType.CANDLESTICK),
    RANGE("Range", false, DataSetType.CANDLESTICK);

    private final String label;
    private final boolean lineRenderer;
    private final DataSetType preferredDataSetType;

    ChartType(String label, boolean lineRenderer, DataSetType preferredDataSetType) {
        this.label = label;
        this.lineRenderer = lineRenderer;
        this.preferredDataSetType = preferredDataSetType;
    }

    public boolean usesLineRenderer() {
        return lineRenderer;
    }

    public DataSetType getPreferredDataSetType() {
        return preferredDataSetType;
    }

    @Override
    public String toString() {
        return label;
    }
}

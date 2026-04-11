package concrete.goonie.chart.api.indicator;

/**
 * Immutable editable indicator property description.
 */
public record IndicatorProperty(
        String key,
        String label,
        IndicatorPropertyType type,
        String value,
        boolean editable
) {}

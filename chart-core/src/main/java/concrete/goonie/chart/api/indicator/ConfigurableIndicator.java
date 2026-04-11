package concrete.goonie.chart.api.indicator;

import java.util.List;

/**
 * Optional indicator editing contract for chart property panels.
 */
public interface ConfigurableIndicator {
    List<IndicatorProperty> getProperties();
    boolean applyProperty(String key, String rawValue);
}

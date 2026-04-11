package concrete.goonie.chart.api.pane;

import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.indicator.Indicator;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.api.render.AxisRangePolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configurable auxiliary chart pane that shares X alignment with the main chart while keeping its own Y viewport.
 */
public final class ChartPane {
    private final String id;
    private final String title;
    private PaneLocation location = PaneLocation.OVERLAY_BOTTOM;
    private double heightRatio = 0.24;
    private boolean visible = true;
    private boolean autoFitY = true;
    private final List<DataSet> dataSets = new ArrayList<>();
    private final List<Indicator> indicators = new ArrayList<>();
    private Viewport viewport;
    private AxisRangePolicy yAxisRangePolicy = new AxisRangePolicy();
    private final Set<Timeframe> visibleTimeframes = new LinkedHashSet<>();

    public ChartPane(String id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public PaneLocation getLocation() { return location; }
    public ChartPane setLocation(PaneLocation location) { this.location = location == null ? PaneLocation.OVERLAY_BOTTOM : location; return this; }
    public double getHeightRatio() { return heightRatio; }
    public ChartPane setHeightRatio(double heightRatio) { this.heightRatio = Math.max(0.10, Math.min(0.60, heightRatio)); return this; }
    public boolean isVisible() { return visible; }
    public ChartPane setVisible(boolean visible) { this.visible = visible; return this; }
    public boolean isAutoFitY() { return autoFitY; }
    public ChartPane setAutoFitY(boolean autoFitY) { this.autoFitY = autoFitY; return this; }
    public Viewport getViewport() { return viewport; }
    public void setViewport(Viewport viewport) { this.viewport = viewport; }

    public AxisRangePolicy getYAxisRangePolicy() {
        return yAxisRangePolicy;
    }

    public ChartPane setYAxisRangePolicy(AxisRangePolicy yAxisRangePolicy) {
        this.yAxisRangePolicy = yAxisRangePolicy == null ? new AxisRangePolicy() : yAxisRangePolicy;
        return this;
    }

    public ChartPane addDataSet(DataSet dataSet) {
        if (dataSet != null && !dataSets.contains(dataSet)) {
            dataSets.add(dataSet);
        }
        return this;
    }

    public ChartPane addIndicator(Indicator indicator) {
        if (indicator != null && !indicators.contains(indicator)) {
            indicators.add(indicator);
        }
        return this;
    }

    public List<DataSet> getDataSets() { return Collections.unmodifiableList(dataSets); }
    public List<Indicator> getIndicators() { return Collections.unmodifiableList(indicators); }

    public Set<Timeframe> getVisibleTimeframes() {
        return Collections.unmodifiableSet(visibleTimeframes);
    }

    public boolean isVisibleOnTimeframe(Timeframe timeframe) {
        return timeframe != null && (visibleTimeframes.isEmpty() || visibleTimeframes.contains(timeframe));
    }

    public ChartPane setVisibleOnTimeframe(Timeframe timeframe, boolean visible) {
        if (timeframe == null) {
            return this;
        }
        if (visible) {
            visibleTimeframes.add(timeframe);
        } else {
            visibleTimeframes.remove(timeframe);
        }
        return this;
    }

    public ChartPane setVisibleTimeframes(Timeframe... timeframes) {
        visibleTimeframes.clear();
        if (timeframes != null) {
            Collections.addAll(visibleTimeframes, timeframes);
        }
        return this;
    }

    public boolean supports(Timeframe timeframe) {
        if (!isVisibleOnTimeframe(timeframe)) {
            return false;
        }
        for (DataSet dataSet : dataSets) {
            if (dataSet.isVisible() && dataSet.supports(timeframe)) return true;
        }
        for (Indicator indicator : indicators) {
            if (indicator.supports(timeframe)) return true;
        }
        return false;
    }
}

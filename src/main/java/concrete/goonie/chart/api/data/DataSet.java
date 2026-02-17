package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Base contract for a renderable chart dataset.
 */
public abstract class DataSet {
    private final String name;
    private final DataSetType type;
    private final Set<Timeframe> selectedTimeframes = new LinkedHashSet<>();
    private boolean visible = true;

    protected DataSet(String name, DataSetType type, Timeframe... timeframes) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        if (timeframes != null && timeframes.length > 0) {
            selectedTimeframes.addAll(Arrays.asList(timeframes));
        }
    }

    public final String getName() {
        return name;
    }

    public final DataSetType getType() {
        return type;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final void setSelectedTimeframes(Timeframe... timeframes) {
        selectedTimeframes.clear();
        if (timeframes != null) {
            selectedTimeframes.addAll(Arrays.asList(timeframes));
        }
    }

    public final boolean supports(Timeframe timeframe) {
        return selectedTimeframes.isEmpty() || selectedTimeframes.contains(timeframe);
    }

    public final Set<Timeframe> getSelectedTimeframes() {
        return Set.copyOf(selectedTimeframes);
    }

    public abstract void render(RenderList out, Transform transform, int visibleFrom, int visibleTo);
}

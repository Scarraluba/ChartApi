package concrete.goonie.chart.api.data;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A renderable dataset composed of multiple child datasets.
 */
public final class CompositeDataSet extends DataSet {
    private final List<DataSet> children = new ArrayList<>();

    public CompositeDataSet(String name, Timeframe... timeframes) {
        super(name, DataSetType.RECT, timeframes);
    }

    public CompositeDataSet add(DataSet dataSet) {
        if (dataSet != null) {
            children.add(dataSet);
        }
        return this;
    }

    public List<DataSet> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void render(RenderList out, Transform transform, int visibleFrom, int visibleTo) {
        for (DataSet child : children) {
            if (child != null && child.isVisible()) {
                child.render(out, transform, visibleFrom, visibleTo);
            }
        }
    }
}

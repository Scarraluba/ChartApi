package concrete.goonie.chart.api.tool;

import concrete.goonie.chart.api.model.Timeframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable tool collection.
 */
public final class ToolSet {
    private final List<Tool> tools = new ArrayList<>();

    public void add(Tool tool) {
        if (tool != null && !tools.contains(tool)) {
            tools.add(tool);
        }
    }

    public void remove(Tool tool) {
        tools.remove(tool);
    }

    public void clear() {
        tools.clear();
    }

    public List<Tool> all() {
        return Collections.unmodifiableList(tools);
    }

    public List<Tool> renderableFor(Timeframe timeframe) {
        List<Tool> out = new ArrayList<>();
        for (Tool tool : tools) {
            if (tool.supports(timeframe)) out.add(tool);
        }
        return out;
    }
}

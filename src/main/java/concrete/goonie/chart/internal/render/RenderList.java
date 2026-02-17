package concrete.goonie.chart.internal.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Internal list of render commands. */
public final class RenderList {
    private final List<RenderCommand> commands = new ArrayList<>();

    public void add(RenderCommand command) {
        commands.add(command);
    }

    public void clear() {
        commands.clear();
    }

    public void addAll(List<RenderCommand> values) {
        commands.addAll(values);
    }

    public int size() {
        return commands.size();
    }

    public List<RenderCommand> items() {
        return Collections.unmodifiableList(commands);
    }
}

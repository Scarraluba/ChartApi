package concrete.goonie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RenderList {
    private final List<RenderCommand> commands = new ArrayList<>();

    public void add(RenderCommand cmd) {
        commands.add(cmd);
    }

    public void clear() {
        commands.clear();
    }

    public List<RenderCommand> items() {
        return Collections.unmodifiableList(commands);
    }

    public int size() { return commands.size(); }
}
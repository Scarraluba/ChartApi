package concrete.goonie.chart.api.tool;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Base drawing tool rendered on the chart.
 * <p>
 * Tools can be limited to specific timeframes, dragged as a whole, and expose draggable control points.
 * X coordinates are index-based by default so tool anchors align with chart indexing.
 */
public abstract class Tool {
    private final String name;
    private final Set<Timeframe> selectedTimeframes = new LinkedHashSet<>();
    private boolean visible = true;
    private boolean selected;
    private boolean hovered;
    private boolean snapToIndex = true;

    protected Tool(String name, Timeframe... selectedTimeframes) {
        this.name = name;
        if (selectedTimeframes != null && selectedTimeframes.length > 0) {
            this.selectedTimeframes.addAll(Arrays.asList(selectedTimeframes));
        }
    }

    public final String getName() {
        return name;
    }

    public final boolean supports(Timeframe timeframe) {
        return selectedTimeframes.isEmpty() || selectedTimeframes.contains(timeframe);
    }

    public final Set<Timeframe> getSelectedTimeframes() {
        return Set.copyOf(selectedTimeframes);
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final boolean isSelected() {
        return selected;
    }

    public final void setSelected(boolean selected) {
        this.selected = selected;
    }

    public final boolean isHovered() {
        return hovered;
    }

    public final void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public final boolean isSnapToIndex() {
        return snapToIndex;
    }

    public final void setSnapToIndex(boolean snapToIndex) {
        this.snapToIndex = snapToIndex;
    }

    protected final double alignX(double xWorld) {
        return snapToIndex ? Math.rint(xWorld) : xWorld;
    }

    protected final ControlPoint point(double xWorld, double yWorld, String role) {
        return new ControlPoint(alignX(xWorld), yWorld, role);
    }

    public abstract void render(RenderList out, Transform transform);

    public abstract boolean hitTest(Transform transform, int screenX, int screenY);

    public abstract void translate(double dxWorld, double dyWorld);

    public abstract String tooltip();

    /**
     * Exposes draggable tool anchors in world coordinates.
     */
    public List<ControlPoint> controlPoints() {
        return List.of();
    }

    /**
     * Moves a specific control point.
     */
    public void moveControlPoint(int index, double xWorld, double yWorld) {
        // optional
    }

    public int hitTestControlPoint(Transform transform, int screenX, int screenY, double radiusPx) {
        List<ControlPoint> points = controlPoints();
        for (int i = 0; i < points.size(); i++) {
            ControlPoint point = points.get(i);
            int sx = transform.worldToScreenX(point.xWorld());
            int sy = transform.worldToScreenY(point.yWorld());
            if (Math.hypot(screenX - sx, screenY - sy) <= radiusPx) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Immutable draggable handle definition.
     */
    public record ControlPoint(double xWorld, double yWorld, String role) {}
}

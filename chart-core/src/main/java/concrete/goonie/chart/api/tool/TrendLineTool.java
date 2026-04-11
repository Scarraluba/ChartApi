package concrete.goonie.chart.api.tool;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

import java.util.List;

public final class TrendLineTool extends Tool {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public TrendLineTool(String name, double x1, double y1, double x2, double y2, Timeframe... selectedTimeframes) {
        super(name, selectedTimeframes);
        this.x1 = alignX(x1);
        this.y1 = y1;
        this.x2 = alignX(x2);
        this.y2 = y2;
    }

    @Override
    public void render(RenderList out, Transform t) {
        int sx1 = t.worldToScreenX(x1);
        int sy1 = t.worldToScreenY(y1);
        int sx2 = t.worldToScreenX(x2);
        int sy2 = t.worldToScreenY(y2);

        int lineColor = isSelected() ? 0xFF67E8F9 : isHovered() ? 0xFF22D3EE : 0xFF0891B2;
        out.add(new RenderCommand.Line(sx1, sy1, sx2, sy2, new Style(lineColor, isSelected() ? 2.4f : 2f, false)));

        for (int i = 0; i < controlPoints().size(); i++) {
            ControlPoint point = controlPoints().get(i);
            float radius = isSelected() ? 6f : 5f;
            int fill = isSelected() ? 0xFFFFFFFF : 0xFFBAE6FD;
            int border = isHovered() || isSelected() ? 0xFF22D3EE : 0xFF7DD3FC;
            out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), radius, new Style(fill, 1f, true)));
            out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), radius + 0.8f, new Style(border, 1.5f, false)));
        }

        out.add(new RenderCommand.Text(sx1 + 8, sy1 - 10, getName(), 11f, new Style(0xFFE5E7EB, 1f, false)));
    }

    @Override
    public boolean hitTest(Transform t, int screenX, int screenY) {
        double sx1 = t.worldToScreenX(x1);
        double sy1 = t.worldToScreenY(y1);
        double sx2 = t.worldToScreenX(x2);
        double sy2 = t.worldToScreenY(y2);
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;
        double len2 = dx * dx + dy * dy;
        if (len2 <= 1e-9) {
            return false;
        }
        double factor = ((screenX - sx1) * dx + (screenY - sy1) * dy) / len2;
        factor = Math.max(0.0, Math.min(1.0, factor));
        double px = sx1 + factor * dx;
        double py = sy1 + factor * dy;
        return Math.hypot(screenX - px, screenY - py) <= 7.0;
    }


    @Override
    public double hitTestDistancePx(Transform t, int screenX, int screenY) {
        double sx1 = t.worldToScreenX(x1);
        double sy1 = t.worldToScreenY(y1);
        double sx2 = t.worldToScreenX(x2);
        double sy2 = t.worldToScreenY(y2);
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;
        double len2 = dx * dx + dy * dy;
        if (len2 <= 1e-9) {
            return Double.POSITIVE_INFINITY;
        }
        double factor = ((screenX - sx1) * dx + (screenY - sy1) * dy) / len2;
        factor = Math.max(0.0, Math.min(1.0, factor));
        double px = sx1 + factor * dx;
        double py = sy1 + factor * dy;
        return Math.hypot(screenX - px, screenY - py);
    }

    @Override
    public void translate(double dxWorld, double dyWorld) {
        x1 = alignX(x1 + dxWorld);
        x2 = alignX(x2 + dxWorld);
        y1 += dyWorld;
        y2 += dyWorld;
    }

    @Override
    public String tooltip() {
        return getName();
    }

    @Override
    public List<ControlPoint> controlPoints() {
        return List.of(point(x1, y1, "start"), point(x2, y2, "end"));
    }

    @Override
    public double controlPointHitRadiusPx() {
        return 10.0;
    }

    @Override
    public void moveControlPoint(int index, double xWorld, double yWorld) {
        if (index == 0) {
            x1 = alignX(xWorld);
            y1 = yWorld;
        } else if (index == 1) {
            x2 = alignX(xWorld);
            y2 = yWorld;
        }
    }
}

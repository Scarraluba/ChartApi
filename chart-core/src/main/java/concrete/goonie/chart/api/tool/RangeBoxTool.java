package concrete.goonie.chart.api.tool;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

import java.util.List;

public final class RangeBoxTool extends Tool {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public RangeBoxTool(String name, double x1, double y1, double x2, double y2, Timeframe... selectedTimeframes) {
        super(name, selectedTimeframes);
        this.x1 = alignX(x1);
        this.y1 = y1;
        this.x2 = alignX(x2);
        this.y2 = y2;
    }

    @Override
    public void render(RenderList out, Transform t) {
        float left = t.worldToScreenX(Math.min(x1, x2));
        float right = t.worldToScreenX(Math.max(x1, x2));
        float top = t.worldToScreenY(Math.max(y1, y2));
        float bottom = t.worldToScreenY(Math.min(y1, y2));

        int fillColor = isSelected() ? 0x334F46E5 : isHovered() ? 0x284F46E5 : 0x224F46E5;
        int borderColor = isSelected() ? 0xFFA5B4FC : isHovered() ? 0xFF818CF8 : 0xFF6366F1;
        out.add(new RenderCommand.RoundedRect(left, top, right, bottom, 8f, new Style(fillColor, 1.2f, true).withCornerRadius(8f)));
        out.add(new RenderCommand.RoundedRect(left, top, right, bottom, 8f, new Style(borderColor, isSelected() ? 1.8f : 1.3f, false).withCornerRadius(8f)));
        out.add(new RenderCommand.Text(left + 8f, top + 15f, getName(), 11f, new Style(0xFFE5E7EB, 1f, false)));

        for (ControlPoint point : controlPoints()) {
            out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), isSelected() ? 6f : 5f, new Style(0xFFFFFFFF, 1f, true)));
            out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), isSelected() ? 6.8f : 5.8f, new Style(borderColor, 1.4f, false)));
        }
    }

    @Override
    public boolean hitTest(Transform t, int screenX, int screenY) {
        float left = t.worldToScreenX(Math.min(x1, x2));
        float right = t.worldToScreenX(Math.max(x1, x2));
        float top = t.worldToScreenY(Math.max(y1, y2));
        float bottom = t.worldToScreenY(Math.min(y1, y2));
        return screenX >= left && screenX <= right && screenY >= top && screenY <= bottom;
    }


    @Override
    public double hitTestDistancePx(Transform t, int screenX, int screenY) {
        float left = t.worldToScreenX(Math.min(x1, x2));
        float right = t.worldToScreenX(Math.max(x1, x2));
        float top = t.worldToScreenY(Math.max(y1, y2));
        float bottom = t.worldToScreenY(Math.min(y1, y2));
        if (screenX >= left && screenX <= right && screenY >= top && screenY <= bottom) {
            double edge = Math.min(Math.min(Math.abs(screenX - left), Math.abs(screenX - right)), Math.min(Math.abs(screenY - top), Math.abs(screenY - bottom)));
            return edge;
        }
        double dx = 0.0;
        if (screenX < left) dx = left - screenX;
        else if (screenX > right) dx = screenX - right;
        double dy = 0.0;
        if (screenY < top) dy = top - screenY;
        else if (screenY > bottom) dy = screenY - bottom;
        return Math.hypot(dx, dy);
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
        return List.of(
                point(x1, y1, "corner-1"),
                point(x2, y1, "corner-2"),
                point(x2, y2, "corner-3"),
                point(x1, y2, "corner-4")
        );
    }

    @Override
    public double controlPointHitRadiusPx() {
        return 10.0;
    }

    @Override
    public void moveControlPoint(int index, double xWorld, double yWorld) {
        double alignedX = alignX(xWorld);
        switch (index) {
            case 0 -> {
                x1 = alignedX;
                y1 = yWorld;
            }
            case 1 -> {
                x2 = alignedX;
                y1 = yWorld;
            }
            case 2 -> {
                x2 = alignedX;
                y2 = yWorld;
            }
            case 3 -> {
                x1 = alignedX;
                y2 = yWorld;
            }
            default -> {
            }
        }
    }
}

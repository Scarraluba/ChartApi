package concrete.goonie.chart.api.tool;

import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

import java.util.List;
import java.util.Locale;

public final class HorizontalLineTool extends Tool {
    private double price;

    public HorizontalLineTool(String name, double price, Timeframe... selectedTimeframes) {
        super(name, selectedTimeframes);
    this.price = price;
    }

    @Override
    public void render(RenderList out, Transform t) {
        int y = t.worldToScreenY(price);
        Style line = new Style(isSelected() ? 0xFFFBBF24 : isHovered() ? 0xFFF59E0B : 0xFFD97706, isSelected() ? 2f : 1.5f, false);
        line.dashed = true;
        out.add(new RenderCommand.Line(t.left(), y, t.right(), y, line));

        float pillLeft = t.left() + 8f;
        out.add(new RenderCommand.RoundedRect(pillLeft, y - 18f, pillLeft + 78f, y + 3f, 9f, new Style(0xCC111827, 1f, true).withCornerRadius(9f)));
        out.add(new RenderCommand.RoundedRect(pillLeft, y - 18f, pillLeft + 78f, y + 3f, 9f, new Style(isSelected() ? 0xFFF59E0B : 0x55F59E0B, 1f, false).withCornerRadius(9f)));
        out.add(new RenderCommand.Text(pillLeft + 10f, y - 4f, getName(), 11f, new Style(0xFFE5E7EB, 1f, false)));

        ControlPoint point = point(t.screenToWorldX((t.left() + t.right()) * 0.5), price, "price");
        out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), isSelected() ? 6f : 5f, new Style(0xFFFFFFFF, 1f, true)));
        out.add(new RenderCommand.Circle(t.worldToScreenX(point.xWorld()), t.worldToScreenY(point.yWorld()), isSelected() ? 6.8f : 5.8f, new Style(0xFFF59E0B, 1.4f, false)));
    }

    @Override
    public boolean hitTest(Transform t, int screenX, int screenY) {
        int y = t.worldToScreenY(price);
        return Math.abs(screenY - y) <= 6 && screenX >= t.left() && screenX <= t.right();
    }


    @Override
    public double hitTestDistancePx(Transform t, int screenX, int screenY) {
        int y = t.worldToScreenY(price);
        if (screenX < t.left() || screenX > t.right()) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs(screenY - y);
    }

    @Override
    public void translate(double dxWorld, double dyWorld) {
        price += dyWorld;
    }

    @Override
    public String tooltip() {
        return getName() + " @ " + String.format(Locale.US, "%.2f", price);
    }

    @Override
    public List<ControlPoint> controlPoints() {
        return List.of(point(0, price, "price"));
    }

    @Override
    public int hitTestControlPoint(Transform transform, int screenX, int screenY, double radiusPx) {
        int sx = (transform.left() + transform.right()) / 2;
        int sy = transform.worldToScreenY(price);
        return Math.hypot(screenX - sx, screenY - sy) <= radiusPx ? 0 : -1;
    }

    @Override
    public double controlPointHitRadiusPx() {
        return 10.0;
    }

    @Override
    public void moveControlPoint(int index, double xWorld, double yWorld) {
        if (index == 0) {
            price = yWorld;
        }
    }
}

package concrete.goonie.chart.internal.render;
public final class Style {
    public int argb = 0xFFFFFFFF;
    public float strokeWidth = 1f;
    public boolean fill;
    public boolean dashed;
    public float cornerRadius = 0f;
    public Style() {}
    public Style(int argb, float strokeWidth, boolean fill) { this.argb = argb; this.strokeWidth = strokeWidth; this.fill = fill; }
    public Style copy() { Style s = new Style(argb, strokeWidth, fill); s.dashed = dashed; s.cornerRadius = cornerRadius; return s; }
    public Style withCornerRadius(float radius) { this.cornerRadius = Math.max(0f, radius); return this; }
}

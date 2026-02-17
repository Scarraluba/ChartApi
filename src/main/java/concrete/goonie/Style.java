package concrete.goonie;

public final class Style {
    // Pure integer ARGB (0xAARRGGBB) so Android & Desktop can map easily.
    public int argb = 0xFFFFFFFF;
    public float strokeWidth = 1f;

    // If true, renderer should fill shapes (rects). For lines/path, fill can be ignored.
    public boolean fill = false;

    // Optional: some renderers may ignore
    public boolean dashed = false;

    public Style() {}

    public Style(int argb, float strokeWidth, boolean fill) {
        this.argb = argb;
        this.strokeWidth = strokeWidth;
        this.fill = fill;
    }

    public Style copy() {
        Style s = new Style();
        s.argb = this.argb;
        s.strokeWidth = this.strokeWidth;
        s.fill = this.fill;
        s.dashed = this.dashed;
        return s;
    }
}
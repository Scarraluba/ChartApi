package concrete.goonie;

public abstract class RenderCommand {

    public final Style style;

    protected RenderCommand(Style style) {
        this.style = style;
    }

    public static final class Line extends RenderCommand {
        public final float x1, y1, x2, y2;
        public Line(float x1, float y1, float x2, float y2, Style style) {
            super(style);
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
    }

    public static final class Rect extends RenderCommand {
        public final float left, top, right, bottom;
        public Rect(float left, float top, float right, float bottom, Style style) {
            super(style);
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
    }

    public static final class Text extends RenderCommand {
        public final float x, y;
        public final String text;
        public final float textSize; // “px-like” (renderer decides scaling)
        public Text(float x, float y, String text, float textSize, Style style) {
            super(style);
            this.x = x; this.y = y;
            this.text = text;
            this.textSize = textSize;
        }
    }
}
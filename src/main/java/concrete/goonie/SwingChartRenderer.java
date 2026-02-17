package concrete.goonie;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;


public final class SwingChartRenderer {

    private final Map<Integer, BasicStroke> strokeCache = new HashMap<>();
    private final Font monoBase = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public void draw(Graphics2D g, RenderList list) {
        for (RenderCommand cmd : list.items()) {
            if (cmd instanceof RenderCommand.Line) {
                drawLine(g, (RenderCommand.Line) cmd);
            } else if (cmd instanceof RenderCommand.Rect) {
                drawRect(g, (RenderCommand.Rect) cmd);
            } else if (cmd instanceof RenderCommand.Text) {
                drawText(g, (RenderCommand.Text) cmd);
            }
        }
    }

    private void drawLine(Graphics2D g, RenderCommand.Line c) {
        applyStyle(g, c.style, false);
        g.drawLine(Math.round(c.x1), Math.round(c.y1), Math.round(c.x2), Math.round(c.y2));
    }

    private void drawRect(Graphics2D g, RenderCommand.Rect c) {
        applyStyle(g, c.style, true);

        int l = Math.round(c.left);
        int t = Math.round(c.top);
        int w = Math.round(c.right - c.left);
        int h = Math.round(c.bottom - c.top);

        if (c.style.fill) g.fillRect(l, t, w, h);
        else g.drawRect(l, t, w, h);
    }

    private void drawText(Graphics2D g, RenderCommand.Text c) {
        g.setColor(new Color(c.style.argb, true));

        int size = Math.max(8, Math.round(c.textSize));
        g.setFont(monoBase.deriveFont((float) size));

        g.drawString(c.text, Math.round(c.x), Math.round(c.y));
    }

    private void applyStyle(Graphics2D g, Style s, boolean isRect) {
        g.setColor(new Color(s.argb, true));

        if (s.fill || isRect) {
            // Fill shapes don't need stroke, but Swing still needs a stroke for lines later.
            // We'll still set stroke for consistency.
        }

        g.setStroke(strokeFor(s.strokeWidth, s.dashed));
    }

    private BasicStroke strokeFor(float width, boolean dashed) {
        int w = Math.max(1, Math.round(width * 10f));
        int key = w ^ (dashed ? 0xBEEF : 0x1234);

        BasicStroke cached = strokeCache.get(key);
        if (cached != null) return cached;

        float realW = Math.max(0.5f, width);

        BasicStroke stroke;
        if (dashed) {
            stroke = new BasicStroke(
                    realW,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    10f,
                    new float[]{6f, 6f},
                    0f
            );
        } else {
            stroke = new BasicStroke(realW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        strokeCache.put(key, stroke);
        return stroke;
    }
}
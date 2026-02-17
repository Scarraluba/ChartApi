package concrete.goonie.chart.swing;
import concrete.goonie.chart.internal.render.*;
import java.awt.*; import java.awt.geom.RoundRectangle2D; import java.util.*;
public final class SwingChartRenderer {
    private final Map<Integer,BasicStroke> strokeCache = new HashMap<>();
    private final Font baseFont = new Font("SansSerif", Font.PLAIN, 12);
    public void draw(Graphics2D g, RenderList list) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (RenderCommand cmd : list.items()) {
            if (cmd instanceof RenderCommand.Line c) drawLine(g,c); else if (cmd instanceof RenderCommand.RoundedRect c) drawRoundedRect(g,c); else if (cmd instanceof RenderCommand.Rect c) drawRect(g,c); else if (cmd instanceof RenderCommand.Circle c) drawCircle(g,c); else if (cmd instanceof RenderCommand.Text c) drawText(g,c);
        }
    }
    private void drawLine(Graphics2D g, RenderCommand.Line c){ applyStyle(g,c.style); g.drawLine(Math.round(c.x1),Math.round(c.y1),Math.round(c.x2),Math.round(c.y2)); }
    private void drawRect(Graphics2D g, RenderCommand.Rect c){ applyStyle(g,c.style); int l=Math.round(c.left), t=Math.round(c.top), w=Math.round(c.right-c.left), h=Math.round(c.bottom-c.top); if(c.style.fill) g.fillRect(l,t,w,h); else g.drawRect(l,t,w,h); }
    private void drawRoundedRect(Graphics2D g, RenderCommand.RoundedRect c){ applyStyle(g,c.style); RoundRectangle2D.Float s=new RoundRectangle2D.Float(c.left,c.top,c.right-c.left,c.bottom-c.top,c.radius*2f,c.radius*2f); if(c.style.fill) g.fill(s); else g.draw(s); }
    private void drawCircle(Graphics2D g, RenderCommand.Circle c){ applyStyle(g,c.style); int d=Math.round(c.radius*2f), x=Math.round(c.cx-c.radius), y=Math.round(c.cy-c.radius); if(c.style.fill) g.fillOval(x,y,d,d); else g.drawOval(x,y,d,d); }
    private void drawText(Graphics2D g, RenderCommand.Text c){ g.setColor(new Color(c.style.argb,true)); g.setFont(baseFont.deriveFont(c.fontStyle, (float)Math.max(8, Math.round(c.textSize)))); g.drawString(c.text,Math.round(c.x),Math.round(c.y)); }
    private void applyStyle(Graphics2D g, Style s){ g.setColor(new Color(s.argb,true)); g.setStroke(strokeFor(s.strokeWidth, s.dashed)); }
    private BasicStroke strokeFor(float width, boolean dashed){ int key=Math.max(1,Math.round(width*10f)) ^ (dashed?0xBEEF:0x1234); BasicStroke c=strokeCache.get(key); if(c!=null) return c; BasicStroke s=dashed ? new BasicStroke(Math.max(0.5f,width),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10f,new float[]{6f,6f},0f) : new BasicStroke(Math.max(0.5f,width),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND); strokeCache.put(key,s); return s; }
}

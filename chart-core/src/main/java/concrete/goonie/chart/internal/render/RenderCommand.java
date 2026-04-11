package concrete.goonie.chart.internal.render;
public abstract class RenderCommand {
    public final Style style;
    protected RenderCommand(Style style) { this.style = style; }
    public static final class Line extends RenderCommand { public final float x1,y1,x2,y2; public Line(float x1,float y1,float x2,float y2,Style style){ super(style); this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2; } }
    public static class Rect extends RenderCommand { public final float left,top,right,bottom; public Rect(float left,float top,float right,float bottom,Style style){ super(style); this.left=left; this.top=top; this.right=right; this.bottom=bottom; } }
    public static final class RoundedRect extends Rect { public final float radius; public RoundedRect(float left,float top,float right,float bottom,float radius,Style style){ super(left,top,right,bottom,style); this.radius=radius; } }
    public static final class Text extends RenderCommand { public final float x,y; public final String text; public final float textSize; public final int fontStyle; public Text(float x,float y,String text,float textSize,Style style){ this(x,y,text,textSize,java.awt.Font.PLAIN,style); } public Text(float x,float y,String text,float textSize,int fontStyle,Style style){ super(style); this.x=x; this.y=y; this.text=text; this.textSize=textSize; this.fontStyle=fontStyle; } }
    public static final class Circle extends RenderCommand { public final float cx,cy,radius; public Circle(float cx,float cy,float radius,Style style){ super(style); this.cx=cx; this.cy=cy; this.radius=radius; } }
    public static final class ClipPush extends RenderCommand { public final float left,top,right,bottom; public ClipPush(float left,float top,float right,float bottom){ super(new Style(0,0f,false)); this.left=left; this.top=top; this.right=right; this.bottom=bottom; } }
    public static final class ClipPop extends RenderCommand { public ClipPop(){ super(new Style(0,0f,false)); } }
}

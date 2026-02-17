package concrete.goonie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Chart extends JPanel {

    private final ChartEngine engine = new ChartEngine();
    private final RenderList renderList = new RenderList();
    private final SwingChartRenderer renderer = new SwingChartRenderer();

    private CandleSeries series;
    private Viewport viewport;

    private int mouseX = -1;
    private int mouseY = -1;

    private boolean dragging;
    private int lastX;
    private int lastY;

    public Chart() {
        setFocusable(true);

        // demo data
        series = CandleSeries.generateDemo(2500);
        initViewport();
        setBackground(new Color(24, 34, 64));
        // mouse move -> crosshair
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

            @Override public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();

                if (dragging) {
                    int dx = e.getX() - lastX;
                    int dy = e.getY() - lastY;
                    lastX = e.getX();
                    lastY = e.getY();

                    panByPixels(dx, dy);
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragging = true;
                    lastX = e.getX();
                    lastY = e.getY();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                dragging = false;
            }

            @Override public void mouseExited(MouseEvent e) {
                mouseX = -1;
                mouseY = -1;
                repaint();
            }
        });

        // zoom: wheel = X zoom, shift+wheel = Y zoom
        addMouseWheelListener(e -> {
            boolean zoomY = e.isShiftDown();
            boolean zoomX = !zoomY;

            zoomAtPixel(e.getX(), e.getY(), e.getWheelRotation(), zoomX, zoomY);
            repaint();
        });

        // reset
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    initViewport();
                    repaint();
                }
            }
        });
    }

    private void initViewport() {
        int n = series.size();
        double xMax = Math.max(1, n - 1);
        double xMin = Math.max(0, xMax - 200);

        int from = (int) Math.max(0, Math.floor(xMin));
        int to = (int) Math.min(n - 1, Math.ceil(xMax));

        double yMin = series.minLow(from, to);
        double yMax = series.maxHigh(from, to);

        double pad = (yMax - yMin) * 0.10;
        yMin -= pad;
        yMax += pad;

        viewport = new Viewport(xMin, xMax, yMin, yMax);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ChartEngine.Params p = new ChartEngine.Params();
            p.viewWidthPx = getWidth();
            p.viewHeightPx = getHeight();

            // crosshair (only if mouse is inside)
            p.drawCrosshair = (mouseX >= 0 && mouseY >= 0);
            p.crosshairScreenX = mouseX;
            p.crosshairScreenY = mouseY;

            engine.build(renderList, series, viewport, p);
            renderer.draw(g, renderList);
        } finally {
            g.dispose();
        }
    }

    // ---- shared-ish math (fine here for now; later you can move into a controller) ----
    private void panByPixels(int dxPixels, int dyPixels) {
        // reproduce the exact padding from ChartEngine.Params defaults
        ChartEngine.Params p = new ChartEngine.Params();
        p.viewWidthPx = getWidth();
        p.viewHeightPx = getHeight();
        ChartEngine.ChartRect cr = engine.computeChartRect(p);

        // use core transform to convert pixels -> world
        Transform t = new Transform(cr.left, cr.top, cr.width, cr.height, viewport);

        double dxWorld = -dxPixels / t.pixelsPerWorldX();
        double dyWorld =  dyPixels / t.pixelsPerWorldY();

        viewport.xMin += dxWorld;
        viewport.xMax += dxWorld;
        viewport.yMin += dyWorld;
        viewport.yMax += dyWorld;

        clampViewport();
    }

    private void zoomAtPixel(int px, int py, int wheelRotation, boolean zoomX, boolean zoomY) {
        ChartEngine.Params p = new ChartEngine.Params();
        p.viewWidthPx = getWidth();
        p.viewHeightPx = getHeight();
        ChartEngine.ChartRect cr = engine.computeChartRect(p);

        Transform t = new Transform(cr.left, cr.top, cr.width, cr.height, viewport);

        double ax = t.screenToWorldX(px);
        double ay = t.screenToWorldY(py);

        double step = 1.12;
        double factor = (wheelRotation > 0) ? step : (1.0 / step);

        if (zoomX) zoomXAt(ax, factor);
        if (zoomY) zoomYAt(ay, factor);

        clampViewport();
    }

    private void zoomXAt(double anchorX, double factor) {
        double minRange = 10;
        double maxRange = Math.max(50, series.size());

        double oldRange = viewport.xRange();
        double newRange = clamp(oldRange * factor, minRange, maxRange);

        double a = (anchorX - viewport.xMin) / oldRange;
        viewport.xMin = anchorX - a * newRange;
        viewport.xMax = viewport.xMin + newRange;
    }

    private void zoomYAt(double anchorY, double factor) {
        double oldRange = viewport.yRange();
        double minRange = 0.5;
        double maxRange = 1e9;

        double newRange = clamp(oldRange * factor, minRange, maxRange);

        double a = (anchorY - viewport.yMin) / oldRange;
        viewport.yMin = anchorY - a * newRange;
        viewport.yMax = viewport.yMin + newRange;
    }

    private void clampViewport() {
        viewport.clampX(0, Math.max(1, series.size() - 1));
        viewport.ensureYNonZero();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
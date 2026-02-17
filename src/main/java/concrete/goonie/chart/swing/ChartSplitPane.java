package concrete.goonie.chart.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Transparent vertical split pane used only for divider interaction and layout.
 * It lets the chart stay visually in one canvas while delegating divider drag
 * behavior to Swing's split pane machinery.
 */
final class ChartSplitPane extends JSplitPane {
    private final PassthroughPanel topPanel = new PassthroughPanel();
    private final PassthroughPanel bottomPanel = new PassthroughPanel();
    private boolean dividerActive;
    private boolean dividerHovered;

    ChartSplitPane(int dividerSize) {
        super(JSplitPane.VERTICAL_SPLIT);
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        setContinuousLayout(true);
        setResizeWeight(0.5);
        setDividerSize(dividerSize);
        setOneTouchExpandable(false);
        setTopComponent(topPanel);
        setBottomComponent(bottomPanel);
        topPanel.setOpaque(false);
        bottomPanel.setOpaque(false);
        setUI(new ChartSplitPaneUi());
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
        divider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                dividerHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!dividerActive) {
                    dividerHovered = false;
                    repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                dividerActive = true;
                dividerHovered = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dividerActive = false;
                repaint();
            }
        });
    }

    void setDividerState(boolean hovered, boolean active) {
        this.dividerHovered = hovered;
        this.dividerActive = active;
        repaint();
    }

    JComponent getTopProxy() {
        return topPanel;
    }

    JComponent getBottomProxy() {
        return bottomPanel;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    private final class ChartSplitPaneUi extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
                @Override
                public void paint(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int width = getWidth();
                        int height = getHeight();
                        int centerY = Math.max(1, height / 2);
                        int glowColor = dividerActive ? 0x4460A5FA : (dividerHovered ? 0x2293C5FD : 0x00000000);
                        int lineColor = dividerActive ? 0xFF60A5FA : (dividerHovered ? 0xFF93C5FD : 0x00000000);
                        if ((glowColor >>> 24) != 0) {
                            g2.setColor(new Color(glowColor, true));
                            g2.fillRoundRect(0, Math.max(0, centerY - 2), width, 4, 4, 4);
                        }
                        if ((lineColor >>> 24) != 0) {
                            g2.setColor(new Color(lineColor, true));
                            g2.fillRect(0, centerY, width, 1);
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };
            divider.setBorder(BorderFactory.createEmptyBorder());
            divider.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            return divider;
        }
    }

    private static final class PassthroughPanel extends JPanel {
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }
}

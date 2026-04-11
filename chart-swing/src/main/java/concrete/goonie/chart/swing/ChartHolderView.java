package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.tool.Tool;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holder around SwingChartPanel with a vertical TradingView-style tool rail and a draggable tool-properties window.
 */
public final class ChartHolderView extends JPanel {
    private final SwingChartPanel chartPanel;
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel leftRail = new JPanel();
    private final ToolPropertiesWindow toolPropertiesWindow;
    private final Map<String, JButton> toolButtons = new LinkedHashMap<>();

    public ChartHolderView(SwingChartPanel chartPanel) {
        super(new BorderLayout());
        this.chartPanel = chartPanel;
        this.toolPropertiesWindow = new ToolPropertiesWindow();
        setOpaque(true);
        setBackground(new Color(0x0B1020));

        buildLeftRail();
        buildLayeredPane();

        add(leftRail, BorderLayout.WEST);
        add(layeredPane, BorderLayout.CENTER);

        chartPanel.addSelectedToolListener(toolPropertiesWindow::bindTool);
    }

    public SwingChartPanel getChartPanel() {
        return chartPanel;
    }

    private void buildLeftRail() {
        leftRail.setLayout(new BoxLayout(leftRail, BoxLayout.Y_AXIS));
        leftRail.setBackground(new Color(11, 16, 28));
        leftRail.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        leftRail.setPreferredSize(new Dimension(54, 10));

        addToolButton("CURSOR", "＋", "Cursor / none");
        addToolButton("TREND", "／", "Trend line");
        addToolButton("HLINE", "─", "Horizontal line");
        addToolButton("RANGE", "▭", "Range box");

        leftRail.add(Box.createVerticalStrut(10));
        JComponent divider = new JPanel();
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setBackground(new Color(58, 71, 96));
        leftRail.add(divider);
        leftRail.add(Box.createVerticalStrut(10));

        JLabel drawLabel = new JLabel("Tools", SwingConstants.CENTER);
        drawLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        drawLabel.setForeground(new Color(164, 177, 204));
        drawLabel.setFont(drawLabel.getFont().deriveFont(Font.PLAIN, 11f));
        leftRail.add(drawLabel);
        leftRail.add(Box.createVerticalGlue());
    }

    private void addToolButton(String type, String label, String tooltip) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.setToolTipText(tooltip);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(40, 40));
        button.setPreferredSize(new Dimension(40, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 18f));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(17, 24, 39));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 71, 96, 120)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        button.addActionListener(e -> {
            String pending = "CURSOR".equals(type) ? null : type;
            if (pending != null && pending.equals(chartPanel.getPendingToolType())) {
                pending = null;
            }
            chartPanel.setPendingToolType(pending);
            refreshToolButtons();
        });
        toolButtons.put(type, button);
        leftRail.add(button);
        leftRail.add(Box.createVerticalStrut(8));
    }

    private void refreshToolButtons() {
        String pending = chartPanel.getPendingToolType();
        for (Map.Entry<String, JButton> entry : toolButtons.entrySet()) {
            boolean active = entry.getKey().equals(pending) || (pending == null && "CURSOR".equals(entry.getKey()));
            JButton button = entry.getValue();
            button.setBackground(active ? new Color(79, 70, 229) : new Color(17, 24, 39));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active ? new Color(129, 140, 248) : new Color(58, 71, 96, 120)),
                    BorderFactory.createEmptyBorder(6, 6, 6, 6)
            ));
        }
    }

    private void buildLayeredPane() {
        layeredPane.setLayout(null);
        layeredPane.setOpaque(true);
        layeredPane.add(chartPanel, Integer.valueOf(0));
        layeredPane.add(toolPropertiesWindow, Integer.valueOf(10));
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                layoutOverlayChildren();
            }
        });
        refreshToolButtons();
    }

    private void layoutOverlayChildren() {
        chartPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        Dimension size = toolPropertiesWindow.getPreferredSize();
        Rectangle current = toolPropertiesWindow.getBounds();
        if (current.width <= 0 || current.height <= 0) {
            toolPropertiesWindow.setBounds(
                    Math.max(12, layeredPane.getWidth() - size.width - 16),
                    16,
                    size.width,
                    size.height
            );
        } else {
            toolPropertiesWindow.setBounds(
                    Math.min(Math.max(0, current.x), Math.max(0, layeredPane.getWidth() - current.width)),
                    Math.min(Math.max(0, current.y), Math.max(0, layeredPane.getHeight() - current.height)),
                    current.width,
                    current.height
            );
        }
    }

    private final class ToolPropertiesWindow extends JPanel {
        private final JLabel titleLabel = new JLabel("Tool Properties");
        private final JLabel subtitleLabel = new JLabel("Select or add a drawing");
        private final JCheckBox snapBox = new JCheckBox("Align X to bar index", true);
        private final JCheckBox visibleBox = new JCheckBox("Visible", true);
        private final JCheckBox magnetBox = new JCheckBox("Magnet snap", true);
        private Tool boundTool;
        private Point dragOffset;

        private ToolPropertiesWindow() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(260, 164));

            JPanel chrome = new JPanel(new BorderLayout());
            chrome.setOpaque(false);
            chrome.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
            subtitleLabel.setForeground(new Color(177, 185, 203));
            subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 11f));

            JPanel textStack = new JPanel();
            textStack.setOpaque(false);
            textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
            textStack.add(titleLabel);
            textStack.add(subtitleLabel);
            header.add(textStack, BorderLayout.WEST);

            JLabel dragLabel = new JLabel("Drag");
            dragLabel.setForeground(new Color(177, 185, 203));
            dragLabel.setFont(dragLabel.getFont().deriveFont(Font.PLAIN, 11f));
            header.add(dragLabel, BorderLayout.EAST);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            snapBox.setOpaque(false);
            snapBox.setForeground(Color.WHITE);
            visibleBox.setOpaque(false);
            visibleBox.setForeground(Color.WHITE);
            magnetBox.setOpaque(false);
            magnetBox.setForeground(Color.WHITE);
            magnetBox.setSelected(chartPanel.isMagnetSnappingEnabled());
            snapBox.addActionListener(e -> {
                if (boundTool != null) {
                    boundTool.setSnapToIndex(snapBox.isSelected());
                    chartPanel.repaint();
                }
            });
            visibleBox.addActionListener(e -> {
                if (boundTool != null) {
                    boundTool.setVisible(visibleBox.isSelected());
                    chartPanel.repaint();
                }
            });
            magnetBox.addActionListener(e -> chartPanel.setMagnetSnappingEnabled(magnetBox.isSelected()));

            JButton clearButton = new JButton("Clear Select");
            clearButton.setFocusable(false);
            clearButton.addActionListener(e -> { chartPanel.setPendingToolType(null); chartPanel.deselectTools(); });

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.add(clearButton);

            body.add(snapBox);
            body.add(Box.createVerticalStrut(8));
            body.add(visibleBox);
            body.add(Box.createVerticalStrut(8));
            body.add(magnetBox);
            body.add(Box.createVerticalStrut(10));
            body.add(row);
            body.add(Box.createVerticalGlue());

            chrome.add(header, BorderLayout.NORTH);
            chrome.add(body, BorderLayout.CENTER);
            add(chrome, BorderLayout.CENTER);

            MouseAdapter dragAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    dragOffset = event.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (dragOffset == null) {
                        return;
                    }
                    Point parentPoint = javax.swing.SwingUtilities.convertPoint(ToolPropertiesWindow.this, event.getPoint(), layeredPane);
                    int x = parentPoint.x - dragOffset.x;
                    int y = parentPoint.y - dragOffset.y;
                    setLocationWithSnap(x, y);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    dragOffset = null;
                    setCursor(Cursor.getDefaultCursor());
                }
            };
            addMouseListener(dragAdapter);
            addMouseMotionListener(dragAdapter);
            bindTool(null);
        }

        private void setLocationWithSnap(int x, int y) {
            int snap = 14;
            int maxX = Math.max(0, layeredPane.getWidth() - getWidth());
            int maxY = Math.max(0, layeredPane.getHeight() - getHeight());
            x = Math.max(0, Math.min(maxX, x));
            y = Math.max(0, Math.min(maxY, y));
            if (x < snap) x = 0;
            if (y < snap) y = 0;
            if (Math.abs(maxX - x) < snap) x = maxX;
            if (Math.abs(maxY - y) < snap) y = maxY;
            setLocation(x, y);
        }

        private void bindTool(Tool tool) {
            this.boundTool = tool;
            boolean hasTool = tool != null;
            titleLabel.setText(hasTool ? tool.getName() : "Tool Properties");
            subtitleLabel.setText(hasTool ? tool.getClass().getSimpleName() + (chartPanel.getSelectedTools().size() > 1 ? " · multi " + chartPanel.getSelectedTools().size() : "") : "Select or add a drawing");
            snapBox.setEnabled(hasTool);
            visibleBox.setEnabled(hasTool);
            snapBox.setSelected(!hasTool || tool.isSnapToIndex());
            visibleBox.setSelected(!hasTool || tool.isVisible());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, new Color(20, 28, 46, 232), 0, getHeight(), new Color(8, 12, 22, 244)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g.setColor(new Color(148, 163, 184, 110));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}

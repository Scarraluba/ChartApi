package concrete.goonie.chart.demo;

import concrete.goonie.chart.api.data.BarDataSet;
import concrete.goonie.chart.api.data.LineDataSet;
import concrete.goonie.chart.api.data.RectDataSet;
import concrete.goonie.chart.api.data.TextDataSet;
import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.history.TimeframeAggregator;
import concrete.goonie.chart.api.indicator.SmaIndicator;
import concrete.goonie.chart.api.indicator.VolumeHistogramIndicator;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.api.pane.ChartPane;
import concrete.goonie.chart.api.pane.PaneLocation;
import concrete.goonie.chart.api.render.AxisRangePolicy;
import concrete.goonie.chart.api.render.AxisXLocation;
import concrete.goonie.chart.api.render.AxisYLocation;
import concrete.goonie.chart.api.render.ChartStyle;
import concrete.goonie.chart.api.render.RangeType;
import concrete.goonie.chart.api.replay.ReplayStartMode;
import concrete.goonie.chart.api.tool.HorizontalLineTool;
import concrete.goonie.chart.api.tool.RangeBoxTool;
import concrete.goonie.chart.api.tool.TrendLineTool;
import concrete.goonie.chart.api.trade.PositionInfo;
import concrete.goonie.chart.swing.ChartHolderView;
import concrete.goonie.chart.api.data.ChartType;
import concrete.goonie.chart.swing.SwingChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HistoricalData historicalData = buildHistoricalData();
            SwingChartPanel chartPanel = new SwingChartPanel();
            chartPanel.setHistoricalData(historicalData);
            chartPanel.setActiveSymbol("DEMO");
            chartPanel.setActiveTimeframe(Timeframe.M15);
            chartPanel.getIndicators().add(new SmaIndicator("SMA 20", Timeframe.M15, 20, Timeframe.M15, Timeframe.M30, Timeframe.H1));
            VolumeHistogramIndicator volume = new VolumeHistogramIndicator("Volume", Timeframe.M15, Timeframe.M15, Timeframe.H1);
            chartPanel.getIndicators().add(volume);
            chartPanel.getTools().add(new HorizontalLineTool("Liquidity", 102.0, Timeframe.M15, Timeframe.H1, Timeframe.H4));
            chartPanel.getTools().add(new TrendLineTool("Trend A", 35, 98.0, 130, 106.0, Timeframe.M15, Timeframe.M30, Timeframe.H1));
            chartPanel.getTools().add(new RangeBoxTool("OB", 150, 99.2, 195, 101.7, Timeframe.M15, Timeframe.H1));
            chartPanel.getCustomDataSets().add(buildLineDataSet());
            chartPanel.getCustomDataSets().add(buildRectDataSet());
            chartPanel.getCustomDataSets().add(buildTextDataSet());
            chartPanel.setBidAsk(101.23, 101.35, 0.01);
            chartPanel.addPosition(new PositionInfo("POS-1", PositionInfo.Side.BUY, 100.80, 99.90, 102.90, 1.0, 0.01));
            chartPanel.addPosition(new PositionInfo("POS-2", PositionInfo.Side.SELL, 102.10, 103.20, 100.70, 0.5, 0.01));

            ChartPane volumeOverlayPane = new ChartPane("overlay-volume", "Volume Overlay")
                    .setLocation(PaneLocation.OVERLAY_BOTTOM)
                    .setHeightRatio(0.24)
                    .addIndicator(volume);
            ChartPane momentumStackPane = new ChartPane("stacked-custom", "Momentum Pane")
                    .setLocation(PaneLocation.STACKED)
                    .setHeightRatio(0.22)
                    .setYAxisRangePolicy(new AxisRangePolicy().setNonNegative(false).setMinAllowed(-1500.0).setMaxAllowed(1500.0))
                    .addDataSet(buildBarDataSet());
            chartPanel.addPane(volumeOverlayPane);
            chartPanel.addPane(momentumStackPane);

            final JFrame[] frameRef = new JFrame[1];
            JFrame frame = new JFrame("Goonie Chart API - Replay + Trading Chart");
            frameRef[0] = frame;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            ChartHolderView chartHolderView = new ChartHolderView(chartPanel);
            frame.add(chartHolderView, BorderLayout.CENTER);
            frame.add(buildControls(chartPanel, frameRef), BorderLayout.NORTH);
            frame.add(buildInfoArea(chartPanel), BorderLayout.EAST);
            frame.setSize(1540, 900);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel buildControls(SwingChartPanel chartPanel, JFrame[] frameRef) {
        JPanel host = new JPanel(new BorderLayout());
        host.setBackground(new Color(ChartStyle.COLOR_BG_TOP, true));
        host.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); row1.setOpaque(false);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); row2.setOpaque(false);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); row3.setOpaque(false);

        JComboBox<Timeframe> timeframeBox = new JComboBox<>(Timeframe.values());
        timeframeBox.setSelectedItem(Timeframe.M15);
        timeframeBox.addActionListener(event -> chartPanel.setActiveTimeframe((Timeframe) timeframeBox.getSelectedItem()));

        JComboBox<AxisXLocation> xAxisBox = new JComboBox<>(AxisXLocation.values());
        xAxisBox.setSelectedItem(AxisXLocation.BOTTOM);
        xAxisBox.addActionListener(event -> chartPanel.setAxisXLocation((AxisXLocation) xAxisBox.getSelectedItem()));

        JComboBox<AxisYLocation> yAxisBox = new JComboBox<>(AxisYLocation.values());
        yAxisBox.setSelectedItem(AxisYLocation.RIGHT);
        yAxisBox.addActionListener(event -> chartPanel.setAxisYLocation((AxisYLocation) yAxisBox.getSelectedItem()));

        JComboBox<ChartType> chartTypeBox = new JComboBox<>(ChartType.values());
        chartTypeBox.setSelectedItem(chartPanel.getMainChartType());
        chartTypeBox.addActionListener(event -> chartPanel.setMainChartType((ChartType) chartTypeBox.getSelectedItem()));

        JCheckBox autoFitBox = new JCheckBox("Auto Fit Y", true);
        autoFitBox.setOpaque(false);
        autoFitBox.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        autoFitBox.addActionListener(event -> chartPanel.setAutoFitY(autoFitBox.isSelected()));

        JCheckBox nonNegativeBox = new JCheckBox("Main Y Non-Negative", false);
        nonNegativeBox.setOpaque(false);
        nonNegativeBox.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        nonNegativeBox.addActionListener(event -> {
            chartPanel.setYAxisRangePolicy(new AxisRangePolicy().setNonNegative(nonNegativeBox.isSelected()).setMinAllowed(null).setMaxAllowed(null).setFixedLowerBound(null).setFixedUpperBound(null));
            if (autoFitBox.isSelected()) chartPanel.fitYNow(); else chartPanel.repaint();
        });

        JButton lockZeroButton = button("Lock Min 0.0");
        lockZeroButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().setFixedLowerBound(0.0)); chartPanel.fitYNow(); });
        JButton fixedRangeButton = button("Y 95..110");
        fixedRangeButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().setFixedLowerBound(95.0).setFixedUpperBound(110.0).setAutoRange(false)); chartPanel.repaint(); });
        JButton zeroBasedButton = button("Zero Based");
        zeroBasedButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().asZeroBased()); chartPanel.fitYNow(); });
        JButton positiveButton = button("Positive");
        positiveButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().asPositive()); chartPanel.fitYNow(); });
        JButton clampedButton = button("Clamp -150..150");
        clampedButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().asClamped(-150.0, 150.0).setRangeType(RangeType.CLAMPED)); chartPanel.fitYNow(); });
        JButton unlockRangeButton = button("Unlock Range");
        unlockRangeButton.addActionListener(event -> { chartPanel.setYAxisRangePolicy(new AxisRangePolicy().asAuto()); chartPanel.fitYNow(); });
        JButton resetButton = button("Reset View");
        resetButton.addActionListener(event -> chartPanel.resetView());
        JButton fitYButton = button("Fit Y Now");
        fitYButton.addActionListener(event -> { chartPanel.fitYNow(); chartPanel.repaint(); });
        JButton addWindowButton = button("Add Window");
        addWindowButton.addActionListener(event -> chartPanel.addRuntimeStackedPane());

        JButton replayButton = button("Replay");
        replayButton.addActionListener(event -> chartPanel.openReplayOverlay());
        JToggleButton replayPanBox = new JToggleButton("Pan X");
        replayPanBox.addActionListener(event -> chartPanel.setReplaySelectionXPanEnabled(replayPanBox.isSelected()));
        JComboBox<ReplayStartMode> startModeBox = new JComboBox<>(ReplayStartMode.values());
        startModeBox.addActionListener(event -> chartPanel.setReplayStartMode((ReplayStartMode) startModeBox.getSelectedItem()));
        JButton playButton = button("Play"); playButton.addActionListener(event -> chartPanel.replayPlay());
        JButton pauseButton = button("Pause"); pauseButton.addActionListener(event -> chartPanel.replayPause());
        JButton restartButton = button("Restart"); restartButton.addActionListener(event -> chartPanel.replayRestart());
        JButton forwardButton = button("Forward"); forwardButton.addActionListener(event -> chartPanel.replayForward());
        JButton liveButton = button("Jump To RealTime"); liveButton.addActionListener(event -> chartPanel.jumpToRealTime());
        JButton translateNewBarsButton = button("Translate New Bars"); translateNewBarsButton.addActionListener(event -> chartPanel.translateViewportByNewBarIndex());
        JComboBox<Integer> speedBox = new JComboBox<>(new Integer[]{1,2,4,8,16});
        speedBox.addActionListener(event -> chartPanel.setReplaySpeed((Integer) speedBox.getSelectedItem()));
        JComboBox<Timeframe> intervalBox = new JComboBox<>(new Timeframe[]{Timeframe.M1, Timeframe.M5, Timeframe.M15});
        intervalBox.addActionListener(event -> chartPanel.setReplayInterval((Timeframe) intervalBox.getSelectedItem()));
        JTextField dateField = new JTextField("2025-01-02T12:00", 16);
        JButton dateApplyButton = button("Set Date");
        dateApplyButton.addActionListener(event -> {
            try { chartPanel.setReplayStartDate(LocalDateTime.parse(dateField.getText())); }
            catch (Exception ignored) { }
        });

        row1.add(label("TF")); row1.add(timeframeBox);
        row1.add(label("Chart")); row1.add(chartTypeBox);
        row1.add(label("X Axis")); row1.add(xAxisBox);
        row1.add(label("Y Axis")); row1.add(yAxisBox);
        row1.add(autoFitBox); row1.add(nonNegativeBox); row1.add(resetButton); row1.add(fitYButton); row1.add(addWindowButton);
        row2.add(lockZeroButton); row2.add(fixedRangeButton); row2.add(zeroBasedButton); row2.add(positiveButton); row2.add(clampedButton); row2.add(unlockRangeButton);
        row3.add(replayButton); row3.add(replayPanBox); row3.add(label("Start")); row3.add(startModeBox); row3.add(dateField); row3.add(dateApplyButton);
        row3.add(playButton); row3.add(pauseButton); row3.add(restartButton); row3.add(forwardButton); row3.add(liveButton); row3.add(translateNewBarsButton);
        JButton fullscreenButton = button("Fullscreen");
        fullscreenButton.addActionListener(event -> toggleFullscreen(frameRef[0], fullscreenButton));

        JButton imageButton = button("Image ▾");
        JPopupMenu imageMenu = new JPopupMenu();
        JMenuItem downloadImageItem = new JMenuItem("Download Image");
        downloadImageItem.addActionListener(event -> downloadImage(chartPanel, host));
        JMenuItem copyImageItem = new JMenuItem("Copy Image");
        copyImageItem.addActionListener(event -> chartPanel.copySnapshotToClipboard());
        imageMenu.add(downloadImageItem);
        imageMenu.add(copyImageItem);
        imageButton.addActionListener(event -> imageMenu.show(imageButton, 0, imageButton.getHeight()));

        row3.add(label("Speed")); row3.add(speedBox); row3.add(label("Interval")); row3.add(intervalBox);
        row3.add(fullscreenButton); row3.add(imageButton);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.add(row1); stack.add(row2); stack.add(row3);
        host.add(stack, BorderLayout.CENTER);
        return host;
    }

    private static JLabel label(String text) { JLabel label = new JLabel(text); label.setForeground(new Color(ChartStyle.COLOR_TEXT_MUTED, true)); return label; }
    private static JButton button(String text) { JButton button = new JButton(text); button.setFocusPainted(false); return button; }

    private static void toggleFullscreen(JFrame frame, JButton button) {
        if (frame == null) return;
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean entering = device.getFullScreenWindow() != frame;
        frame.dispose();
        frame.setUndecorated(entering);
        if (entering) {
            device.setFullScreenWindow(frame);
            frame.setVisible(true);
            button.setText("Minimize");
        } else {
            device.setFullScreenWindow(null);
            frame.setVisible(true);
            frame.setExtendedState(JFrame.NORMAL);
            frame.setLocationRelativeTo(null);
            button.setText("Fullscreen");
        }
    }

    private static void downloadImage(SwingChartPanel chartPanel, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("chart.png"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                chartPanel.saveSnapshot(chooser.getSelectedFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "Save Image", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JScrollPane buildInfoArea(SwingChartPanel chartPanel) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        area.setBackground(new Color(ChartStyle.COLOR_BG, true));
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        area.setPreferredSize(new Dimension(340, 0));
        area.setText("Loaded layers:\n" + String.join("\n", chartPanel.describeVisibleLayers()) + "\n\nReplay:\n- click Replay\n- thick vertical line tracks cursor on main chart\n- click a bar to choose start\n- bars on the right are hidden\n- play / pause / restart / forward / jump to realtime\n- interval feed is M1 / M5 / M15 and independent from chart timeframe\n\nTrading overlays:\n- bid / ask lines\n- OP / SL / TP lines\n- pip difference labels\n");
        return new JScrollPane(area);
    }

    private static HistoricalData buildHistoricalData() {
        HistoricalData historicalData = new HistoricalData();
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        CandleSeries m1 = CandleSeries.generateDemo(start, Timeframe.M1, 6000);
        historicalData.setSeries("DEMO", Timeframe.M1, m1);
        historicalData.setSeries("DEMO", Timeframe.M5, TimeframeAggregator.aggregate(m1, Timeframe.M1, Timeframe.M5));
        historicalData.setSeries("DEMO", Timeframe.M15, TimeframeAggregator.aggregate(m1, Timeframe.M1, Timeframe.M15));
        historicalData.setSeries("DEMO", Timeframe.M30, TimeframeAggregator.aggregate(m1, Timeframe.M1, Timeframe.M30));
        historicalData.setSeries("DEMO", Timeframe.H1, TimeframeAggregator.aggregate(m1, Timeframe.M1, Timeframe.H1));
        historicalData.setSeries("DEMO", Timeframe.H4, TimeframeAggregator.aggregate(m1, Timeframe.M1, Timeframe.H4));
        return historicalData;
    }

    private static LineDataSet buildLineDataSet() {
        int count = 300; double[] x = new double[count]; double[] y = new double[count];
        for (int i = 0; i < count; i++) { x[i] = i; y[i] = 100.0 + Math.sin(i * 0.08) * 6.5; }
        return new LineDataSet("Custom Oscillation", x, y, Timeframe.M15, Timeframe.H1);
    }

    private static BarDataSet buildBarDataSet() {
        int count = 300; double[] x = new double[count]; double[] y = new double[count];
        for (int i = 0; i < count; i++) { x[i] = i; y[i] = Math.cos(i * 0.10) * 1200.0; }
        return new BarDataSet("Custom Histogram", x, y, 0.0, Timeframe.M15, Timeframe.H1);
    }

    private static RectDataSet buildRectDataSet() {
        return new RectDataSet("Zones", new double[]{45, 120}, new double[]{104.4, 99.5}, new double[]{70, 150}, new double[]{102.7, 97.9}, new String[]{"Supply", "Demand"}, Timeframe.M15, Timeframe.H1);
    }

    private static TextDataSet buildTextDataSet() {
        return new TextDataSet("Annotations", new double[]{28, 94, 180}, new double[]{98.7, 103.1, 100.4}, new String[]{"BOS", "CHOCH", "MIT"}, Timeframe.M15, Timeframe.H1);
    }
}

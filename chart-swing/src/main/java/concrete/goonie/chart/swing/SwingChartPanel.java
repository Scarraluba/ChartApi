package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.data.ChartType;
import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.data.DataSetCollection;
import concrete.goonie.chart.api.data.LineDataSet;
import concrete.goonie.chart.api.data.ValueDataSet;
import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.indicator.Indicator;
import concrete.goonie.chart.api.indicator.IndicatorSet;
import concrete.goonie.chart.api.indicator.IndicatorProperty;
import concrete.goonie.chart.api.indicator.ConfigurableIndicator;
import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;
import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.api.pane.ChartPane;
import concrete.goonie.chart.api.replay.ReplayStartMode;
import concrete.goonie.chart.api.replay.ReplayState;
import concrete.goonie.chart.api.replay.RunMode;
import concrete.goonie.chart.api.trade.PositionInfo;
import concrete.goonie.chart.api.perf.RenderOptimizationOptions;
import concrete.goonie.chart.api.pane.PaneLocation;
import concrete.goonie.chart.api.render.AxisXLocation;
import concrete.goonie.chart.api.render.AxisYLocation;
import concrete.goonie.chart.api.render.AxisRangePolicy;
import concrete.goonie.chart.api.render.ChartEngine;
import concrete.goonie.chart.api.render.ChartStyle;
import concrete.goonie.chart.api.tool.Tool;
import concrete.goonie.chart.api.tool.ToolSet;
import concrete.goonie.chart.internal.render.RenderCommand;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Style;
import concrete.goonie.chart.internal.render.Transform;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JMenuItem;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;
import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.Timer;

/**
 * Swing chart panel with main chart + overlay/stacked auxiliary panes that keep exact X alignment.
 */
public final class SwingChartPanel extends JPanel {
    private final ChartEngine chartEngine = new ChartEngine();
    private final RenderList renderList = new RenderList();
    private final SwingChartRenderer swingChartRenderer = new SwingChartRenderer();
    private final ChartEngine.Params params = new ChartEngine.Params();
    private final IndicatorSet indicators = new IndicatorSet();
    private final ToolSet tools = new ToolSet();
    private final DataSetCollection customDataSets = new DataSetCollection();
    private final List<ChartPane> panes = new ArrayList<>();
    private final List<PaneLayout> paneLayouts = new ArrayList<>();
    private final Map<String, PaneWindowState> paneWindowStates = new HashMap<>();
    private int runtimePaneCounter = 1;

    private HistoricalData historicalData = new HistoricalData();
    private String activeSymbol = "DEMO";
    private Timeframe activeTimeframe = Timeframe.M15;
    private CandlestickDataSet mainDataSet;
    private Viewport viewport;
    private boolean autoFitY = true;
    private TooltipInfo tooltipInfo;
    private AxisXLocation axisXLocation = AxisXLocation.BOTTOM;
    private AxisYLocation axisYLocation = AxisYLocation.RIGHT;
    private AxisRangePolicy xAxisRangePolicy = new AxisRangePolicy();
    private AxisRangePolicy yAxisRangePolicy = new AxisRangePolicy();

    private int mouseX = -1;
    private int mouseY = -1;
    private boolean dragging;
    private int lastX;
    private int lastY;
    private Tool activeDragTool;
    private int activeControlPointIndex = -1;
    private Tool hoveredTool;
    private int hoveredControlPointIndex = -1;
    private PaneLayout activeInteractionPane;
    private final RenderOptimizationOptions optimizationOptions = new RenderOptimizationOptions();
    private final Map<String, List<RenderCommand>> retainedPaneSceneCache = new HashMap<>();
    private final List<PositionInfo> positions = new ArrayList<>();
    private final ReplayState replayState = new ReplayState();
    private RunMode runMode = RunMode.REALTIME;
    private CandleSeries realTimeBaseSeries;
    private CandleSeries currentBaseSeries;
    private HistoricalData activeHistoricalData;
    private double bidPrice = Double.NaN;
    private double askPrice = Double.NaN;
    private double pipSize = 0.01;
    private double mainPaneHeightRatio = 0.52;
    private int mainOrderIndex = 0;
    private final Timer replayTimer;
    private long lastCalculatedIndicatorVersion = Long.MIN_VALUE;
    private ChartType mainChartType = ChartType.CANDLES;
    private final List<Consumer<Tool>> selectedToolListeners = new ArrayList<>();
    private final LinkedHashSet<Tool> selectedTools = new LinkedHashSet<>();
    private Tool selectedTool;
    private String pendingToolType;
    private boolean magnetSnappingEnabled = true;

private static final int STACKED_GAP_PX = 0;
private static final int SPLITTER_HIT_HALF_HEIGHT_PX = 5;
private static final int PANE_HEADER_HEIGHT_PX = 36;
private static final int PANE_HEADER_PADDING_PX = 10;
private static final int HEADER_REVEAL_ALPHA_MAX = 255;
private static final int HEADER_BUTTON_HEIGHT_PX = 22;
private static final int HEADER_BUTTON_GAP_PX = 6;
private static final int MIN_AXIS_Y_VISIBLE_WIDTH_PX = 260;
private static final int MIN_AXIS_Y_VISIBLE_CONTENT_WIDTH_PX = 180;
private static final int MIN_AXIS_X_VISIBLE_HEIGHT_PX = 90;
private static final String MAIN_PANE_KEY = "__MAIN__";
private HeaderActionHit hoveredHeaderAction;
private int hoveredSplitterIndex = -1;
private int activeSplitterIndex = -1;
private boolean resizingStackedPanes;
private boolean replaySelectionPannedThisDrag;

    private static final Map<Integer, String> MONTH_ABBREV = new HashMap<>();

    static {
        MONTH_ABBREV.put(1, "Jan");
        MONTH_ABBREV.put(2, "Feb");
        MONTH_ABBREV.put(3, "Mar");
        MONTH_ABBREV.put(4, "Apr");
        MONTH_ABBREV.put(5, "May");
        MONTH_ABBREV.put(6, "Jun");
        MONTH_ABBREV.put(7, "Jul");
        MONTH_ABBREV.put(8, "Aug");
        MONTH_ABBREV.put(9, "Sep");
        MONTH_ABBREV.put(10, "Oct");
        MONTH_ABBREV.put(11, "Nov");
        MONTH_ABBREV.put(12, "Dec");
    }

    public SwingChartPanel() {
        ToolTipManager.sharedInstance().registerComponent(this);
        setFocusable(true);
        setBackground(new Color(ChartStyle.COLOR_BG, true));

        CandleSeries series = CandleSeries.generateDemo(LocalDateTime.of(2025, 1, 1, 0, 0), Timeframe.M1, 4000);
        historicalData.setSeries(activeSymbol, Timeframe.M1, series);
        loadMainDataSet(activeSymbol, activeTimeframe);
        replayTimer = new Timer(500, event -> replayForward());
        stateForMain();
        installInputHandlers();
    }

    public HistoricalData getHistoricalData() { return historicalData; }
    public IndicatorSet getIndicators() { return indicators; }
    public ToolSet getTools() { return tools; }
    public DataSetCollection getCustomDataSets() { return customDataSets; }
    public List<ChartPane> getPanes() { return Collections.unmodifiableList(panes); }
    public Timeframe getActiveTimeframe() { return activeTimeframe; }
    public RenderOptimizationOptions getOptimizationOptions() { return optimizationOptions; }
    public ChartType getMainChartType() { return mainChartType; }

    public Tool getSelectedTool() {
        return selectedTool;
    }

    public void addSelectedToolListener(Consumer<Tool> listener) {
        if (listener != null && !selectedToolListeners.contains(listener)) {
            selectedToolListeners.add(listener);
            listener.accept(selectedTool);
        }
    }

    public void removeSelectedToolListener(Consumer<Tool> listener) {
        selectedToolListeners.remove(listener);
    }

    public void setPendingToolType(String pendingToolType) {
        this.pendingToolType = pendingToolType;
        repaint();
    }

    public void deselectTools() {
        clearSelection();
        repaint();
    }

    public String getPendingToolType() {
        return pendingToolType;
    }

    public boolean isMagnetSnappingEnabled() {
        return magnetSnappingEnabled;
    }

    public void setMagnetSnappingEnabled(boolean magnetSnappingEnabled) {
        this.magnetSnappingEnabled = magnetSnappingEnabled;
        repaint();
    }

    public Set<Tool> getSelectedTools() {
        return Set.copyOf(selectedTools);
    }

    public void setMainChartType(ChartType mainChartType) {
        this.mainChartType = mainChartType == null ? ChartType.CANDLES : mainChartType;
        repaint();
    }

    private PaneWindowState stateForMain() {
        return paneWindowStates.computeIfAbsent(MAIN_PANE_KEY, key -> new PaneWindowState(false));
    }

    private PaneWindowState stateFor(ChartPane pane) {
        if (pane == null) {
            return stateForMain();
        }
        return paneWindowStates.computeIfAbsent(pane.getId(), key -> new PaneWindowState(false));
    }

    public ChartPane addRuntimeStackedPane() {
        LineDataSet lineDataSet = buildRuntimePaneDataSet(runtimePaneCounter);
        ChartPane pane = new ChartPane("runtime-" + runtimePaneCounter, "Window " + runtimePaneCounter)
                .setLocation(PaneLocation.STACKED)
                .setHeightRatio(0.18)
                .setYAxisRangePolicy(new AxisRangePolicy().setNonNegative(false))
                .addDataSet(lineDataSet);
        runtimePaneCounter++;
        addPane(pane);
        return pane;
    }

    private LineDataSet buildRuntimePaneDataSet(int seed) {
        int count = 280;
        double[] x = new double[count];
        double[] y = new double[count];
        double phase = seed * 0.35;
        for (int i = 0; i < count; i++) {
            x[i] = i;
            y[i] = Math.sin(i * 0.07 + phase) * (25.0 + seed * 1.5) + Math.cos(i * 0.03 + phase) * 12.0;
        }
        return new LineDataSet("Runtime " + seed, x, y, Timeframe.M15, Timeframe.H1);
    }

    public void addPane(ChartPane pane) {
        if (pane != null && !panes.contains(pane)) {
            panes.add(pane);
            stateFor(pane);
            repaint();
        }
    }

    public void clearPanes() {
        panes.clear();
        paneWindowStates.clear();
        stateForMain();
        repaint();
    }

    public void setHistoricalData(HistoricalData historicalData) {
        this.historicalData = historicalData == null ? new HistoricalData() : historicalData;
        this.realTimeBaseSeries = this.historicalData.getOrAggregate(activeSymbol, Timeframe.M1);
        this.currentBaseSeries = realTimeBaseSeries;
        loadMainDataSet(activeSymbol, activeTimeframe);
    }

    public void setActiveSymbol(String activeSymbol) {
        this.activeSymbol = activeSymbol == null ? "DEMO" : activeSymbol;
        this.realTimeBaseSeries = this.historicalData.getOrAggregate(this.activeSymbol, Timeframe.M1);
        this.currentBaseSeries = realTimeBaseSeries;
        loadMainDataSet(this.activeSymbol, activeTimeframe);
    }

    public void setActiveTimeframe(Timeframe timeframe) {
        this.activeTimeframe = timeframe == null ? Timeframe.M15 : timeframe;
        loadMainDataSet(activeSymbol, activeTimeframe);
    }

    public void setAutoFitY(boolean autoFitY) {
        this.autoFitY = autoFitY;
        if (autoFitY) {
            fitYNow();
        }
        repaint();
    }

    public void setAxisXLocation(AxisXLocation axisXLocation) {
        this.axisXLocation = axisXLocation == null ? AxisXLocation.BOTTOM : axisXLocation;
        repaint();
    }

    public void setAxisYLocation(AxisYLocation axisYLocation) {
        this.axisYLocation = axisYLocation == null ? AxisYLocation.RIGHT : axisYLocation;
        repaint();
    }

    public AxisRangePolicy getXAxisRangePolicy() {
        return xAxisRangePolicy;
    }

    public AxisRangePolicy getYAxisRangePolicy() {
        return yAxisRangePolicy;
    }

    public void setXAxisRangePolicy(AxisRangePolicy xAxisRangePolicy) {
        this.xAxisRangePolicy = xAxisRangePolicy == null ? new AxisRangePolicy() : xAxisRangePolicy;
        if (viewport != null) {
            viewport.applyXPolicy(this.xAxisRangePolicy);
            clampX();
        }
        repaint();
    }

    public void setYAxisRangePolicy(AxisRangePolicy yAxisRangePolicy) {
        this.yAxisRangePolicy = yAxisRangePolicy == null ? new AxisRangePolicy() : yAxisRangePolicy;
        if (viewport != null) {
            viewport.applyYPolicy(this.yAxisRangePolicy);
        }
        repaint();
    }

    public void resetView() {
        if (mainDataSet == null) {
            return;
        }
        viewport = createDefaultViewport(mainDataSet.getSeries());
        fitYNow();
        repaint();
    }

    public void fitYNow() {
        if (mainDataSet == null || viewport == null) {
            return;
        }
        fitMainPaneY();
        for (ChartPane pane : panes) {
            if (pane.isVisible() && pane.supports(activeTimeframe)) {
                fitPaneY(pane);
            }
        }
    }

    private void fitMainPaneY() {
        int from = Math.max(0, (int) Math.floor(viewport.getXMin()));
        int to = Math.min(mainDataSet.getSeries().size() - 1, (int) Math.ceil(viewport.getXMax()));
        if (to < from) {
            return;
        }

        double low = mainDataSet.getSeries().minLow(from, to);
        double high = mainDataSet.getSeries().maxHigh(from, to);

        for (DataSet dataSet : mainPaneDataSets()) {
            Range range = extractRange(dataSet, from, to);
            if (range != null) {
                low = Math.min(low, range.low());
                high = Math.max(high, range.high());
            }
        }

        double pad = Math.max(1e-9, (high - low) * 0.10);
        viewport.setY(low - pad, high + pad, yAxisRangePolicy);
    }

    private void fitPaneY(ChartPane pane) {
        int from = Math.max(0, (int) Math.floor(viewport.getXMin()));
        int to = Math.min(mainDataSet.getSeries().size() - 1, (int) Math.ceil(viewport.getXMax()));
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (DataSet dataSet : paneDataSets(pane)) {
            Range range = extractRange(dataSet, from, to);
            if (range != null) {
                low = Math.min(low, range.low());
                high = Math.max(high, range.high());
            }
        }
        if (!Double.isFinite(low) || !Double.isFinite(high)) {
            low = 0.0;
            high = 1.0;
        }
        double pad = Math.max(1e-9, (high - low) * 0.12);
        if (pane.getViewport() == null) {
            Viewport paneViewport = new Viewport(viewport.getXMin(), viewport.getXMax(), low - pad, high + pad);
            paneViewport.applyYPolicy(pane.getYAxisRangePolicy());
            pane.setViewport(paneViewport);
        } else {
            pane.getViewport().setX(viewport.getXMin(), viewport.getXMax(), xAxisRangePolicy);
            pane.getViewport().setY(low - pad, high + pad, pane.getYAxisRangePolicy());
        }
    }

    private void loadMainDataSet(String symbol, Timeframe timeframe) {
        CandleSeries previousSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        Viewport previousViewport = viewport;
        Timeframe previousTimeframe = activeTimeframe;
        CandleSeries sourceBase = currentBaseSeries != null ? currentBaseSeries : historicalData.getOrAggregate(symbol, Timeframe.M1);
        CandleSeries series;
        if (sourceBase != null && !sourceBase.isEmpty()) {
            series = timeframe == Timeframe.M1 ? sourceBase : concrete.goonie.chart.api.history.TimeframeAggregator.aggregate(sourceBase, Timeframe.M1, timeframe);
        } else {
            series = historicalData.getOrAggregate(symbol, timeframe);
        }
        if (series == null || series.isEmpty()) {
            series = CandleSeries.generateDemo(LocalDateTime.of(2025, 1, 1, 0, 0), Timeframe.M1, 2500);
            historicalData.setSeries(symbol, Timeframe.M1, series);
            historicalData.setSeries(symbol, timeframe, series);
            realTimeBaseSeries = historicalData.getOrAggregate(symbol, Timeframe.M1);
            currentBaseSeries = realTimeBaseSeries;
        }
        mainDataSet = new CandlestickDataSet(symbol + " " + timeframe.name(), series, timeframe);
        mainDataSet.setOptimizationOptions(optimizationOptions);
        if (previousSeries != null && previousSeries != series) {
            remapToolsToSeries(previousSeries, series, previousTimeframe, timeframe);
        }
        updateBidAskFromLastCandle(series);
        viewport = createViewportForSeries(series, previousSeries, previousViewport, previousTimeframe, timeframe);
        viewport.applyXPolicy(xAxisRangePolicy);
        clampX();
        viewport.applyYPolicy(yAxisRangePolicy);
        activeHistoricalData = buildActiveHistoricalData(symbol);
        calculateIndicatorsNow();
        fitYNow();
        repaint();
    }


    private HistoricalData buildActiveHistoricalData(String symbol) {
        HistoricalData scoped = new HistoricalData();
        CandleSeries base = currentBaseSeries != null && !currentBaseSeries.isEmpty()
                ? currentBaseSeries
                : historicalData.getOrAggregate(symbol, Timeframe.M1);
        if (base != null && !base.isEmpty()) {
            scoped.setSeries(symbol, Timeframe.M1, base);
        }
        return scoped;
    }

    private void ensureIndicatorsCalculated() {
        if (activeHistoricalData == null) {
            activeHistoricalData = buildActiveHistoricalData(activeSymbol);
        }
        long version = indicators.getMutationVersion();
        if (version != lastCalculatedIndicatorVersion) {
            calculateIndicatorsNow();
        }
    }

    private void calculateIndicatorsNow() {
        if (activeHistoricalData == null) {
            activeHistoricalData = buildActiveHistoricalData(activeSymbol);
        }
        indicators.calculateAll(activeHistoricalData, activeSymbol);
        lastCalculatedIndicatorVersion = indicators.getMutationVersion();
    }

    private int currentReplayOverlaySourceIndex() {
        if (mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
            return -1;
        }
        if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && mouseX >= 0) {
            Transform sharedTransform = getSharedReplayTransform(getSharedReplayBounds());
            if (sharedTransform != null) {
                return clampSourceIndexForScreenX(mouseX, sharedTransform);
            }
        }
        if (replayState.getTrackedSourceIndex() >= 0) {
            return clampTrackedSourceIndex(replayState.getTrackedSourceIndex());
        }
        if (replayState.getSelectedSourceIndex() >= 0) {
            return clampTrackedSourceIndex(replayState.getSelectedSourceIndex());
        }
        return clampSourceIndexForViewportRightEdge();
    }

    private double worldXForSourceIndex(int sourceIndex, Timeframe targetTimeframe) {
        if (realTimeBaseSeries == null || realTimeBaseSeries.isEmpty()) {
            return sourceIndex;
        }
        int safe = Math.max(0, Math.min(realTimeBaseSeries.size() - 1, sourceIndex));
        LocalDateTime time = realTimeBaseSeries.get(safe).time();
        CandleSeries targetSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        return worldXForSeriesTime(targetSeries, targetTimeframe, time);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return tooltipInfo == null ? null : tooltipInfo.text();
    }

    @Override
    protected void paintComponent(Graphics graphics0) {
        ensureIndicatorsCalculated();
        super.paintComponent(graphics0);
        if (mainDataSet == null || viewport == null) {
            return;
        }

        syncPaneViewports();
        rebuildPaneLayouts();

        Graphics2D graphics = (Graphics2D) graphics0.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setPaint(new GradientPaint(0, 0, new Color(ChartStyle.COLOR_BG_TOP, true), 0, getHeight(), new Color(ChartStyle.COLOR_BG, true)));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            paintStackedAxisBackgrounds(graphics);
            for (PaneLayout layout : paneLayouts) {
                if (!layout.overlay()) {
                    Rectangle bounds = layout.bounds();
                    Color topColor = layout.main() ? new Color(14, 20, 32, 235) : new Color(18, 26, 40, 235);
                    Color bottomColor = layout.main() ? new Color(8, 12, 22, 245) : new Color(10, 16, 28, 245);
                    graphics.setPaint(new GradientPaint(bounds.x, bounds.y, topColor, bounds.x, bounds.y + bounds.height, bottomColor));
                    graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            }
            renderList.clear();

            for (PaneLayout layout : paneLayouts) {
                paintPane(layout);
            }

            paintReplaySelectionOverlay(renderList);
            paintGlobalXLayer(renderList);
            paintStackedSplitters(renderList);

            if (tooltipInfo != null) {
                paintTooltip(renderList, tooltipInfo);
            }
            swingChartRenderer.draw(graphics, renderList);
            paintWindowChrome(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private void syncPaneViewports() {
        for (ChartPane pane : panes) {
            if (!pane.isVisible() || !pane.supports(activeTimeframe)) {
                continue;
            }
            if (pane.getViewport() == null) {
                fitPaneY(pane);
            } else {
                pane.getViewport().setX(viewport.getXMin(), viewport.getXMax(), xAxisRangePolicy);
                if (pane.isAutoFitY()) {
                    fitPaneY(pane);
                }
            }
        }
    }

    private void rebuildPaneLayouts() {
        paneLayouts.clear();
        retainedPaneSceneCache.clear();
        if (mainDataSet == null) {
            return;
        }

        ParamsBox box = computeSharedBox();
        List<ChartPane> stacked = new ArrayList<>();
        List<ChartPane> overlays = new ArrayList<>();
        for (ChartPane pane : panes) {
            if (!pane.isVisible() || !pane.supports(activeTimeframe)) {
                continue;
            }
            if (pane.getLocation() == PaneLocation.STACKED) {
                stacked.add(pane);
            } else {
                overlays.add(pane);
            }
        }

        int totalSegmentGap = Math.max(0, stacked.size()) * STACKED_GAP_PX;
        int totalSegmentHeight = Math.max(0, box.contentHeight() - totalSegmentGap);
        syncMainPaneRatio(stacked);

        List<Object> segments = new ArrayList<>();
        int clampedMainIndex = Math.max(0, Math.min(mainOrderIndex, stacked.size()));
        for (int i = 0; i < stacked.size() + 1; i++) {
            if (i == clampedMainIndex) {
                segments.add(null);
            }
            if (i < stacked.size()) {
                segments.add(stacked.get(i));
            }
        }
        List<Integer> minHeights = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Object segment : segments) {
            if (segment == null) {
                minHeights.add(getMainPaneMinHeightPx());
                weights.add(Math.max(0.0001, mainPaneHeightRatio));
            } else {
                ChartPane pane = (ChartPane) segment;
                PaneWindowState state = stateFor(pane);
                minHeights.add(state.collapsed() ? PANE_HEADER_HEIGHT_PX : getStackedPaneMinHeightPx());
                weights.add(Math.max(0.0001, pane.getHeightRatio()));
            }
        }
        List<Integer> segmentHeights = StackedPaneSplitLayout.computeHeights(mainPaneHeightRatio, stacked, totalSegmentHeight, STACKED_GAP_PX, minHeights);

        int y = box.top();
        for (int i = 0; i < segments.size(); i++) {
            Object segment = segments.get(i);
            int segmentHeight = segmentHeights.get(i);
            Rectangle bounds = new Rectangle(box.left(), y, box.width(), segmentHeight);
            if (segment == null) {
                Rectangle mainHeader = new Rectangle(bounds.x, bounds.y, bounds.width, 0);
                Rectangle mainContent = new Rectangle(bounds.x, bounds.y, bounds.width, Math.max(1, bounds.height));
                paneLayouts.add(new PaneLayout(null, "Main", bounds, mainContent, mainHeader, viewport, true, false));
            } else {
                ChartPane pane = (ChartPane) segment;
                PaneWindowState state = stateFor(pane);
                Rectangle headerBounds = new Rectangle(bounds.x, bounds.y, bounds.width, Math.min(PANE_HEADER_HEIGHT_PX, bounds.height));
                int contentY = bounds.y;
                int contentHeight = state.collapsed() ? 0 : Math.max(1, bounds.height);
                Rectangle contentBounds = new Rectangle(bounds.x, contentY, bounds.width, contentHeight);
                paneLayouts.add(new PaneLayout(pane, pane.getTitle(), bounds, contentBounds, headerBounds, pane.getViewport(), false, false));
            }
            y += segmentHeight + (i == segments.size() - 1 ? 0 : STACKED_GAP_PX);
        }

        Rectangle mainRect = paneLayouts.stream().filter(PaneLayout::main).findFirst().map(PaneLayout::bounds).orElse(new Rectangle(box.left(), box.top(), box.width(), totalSegmentHeight));
        int overlayIndex = 0;
        for (ChartPane pane : overlays) {
            int overlayInset = 8 + overlayIndex * 10;
            int maxOverlayHeight = Math.max(48, mainRect.height - overlayInset - 4);
            int overlayHeight = Math.min(maxOverlayHeight, Math.max(84, (int) Math.round(mainRect.height * pane.getHeightRatio())));
            int overlayLeft = mainRect.x;
            int overlayTop = Math.max(mainRect.y + 4, mainRect.y + mainRect.height - overlayHeight - overlayInset);
            Rectangle bounds = new Rectangle(overlayLeft, overlayTop, mainRect.width, overlayHeight);
            paneLayouts.add(new PaneLayout(pane, pane.getTitle(), bounds, bounds, new Rectangle(bounds.x, bounds.y, bounds.width, 0), pane.getViewport(), false, true));
            overlayIndex++;
        }
    }

    private List<Integer> computeSegmentHeights(List<Double> weights, List<Integer> minHeights, int totalHeight) {
        List<Integer> heights = new ArrayList<>();
        if (weights.isEmpty()) {
            return heights;
        }
        double totalWeight = 0.0;
        for (double weight : weights) {
            totalWeight += Math.max(0.0001, weight);
        }
        int used = 0;
        for (int i = 0; i < weights.size(); i++) {
            int min = Math.max(1, minHeights.get(i));
            int size = (i == weights.size() - 1)
                    ? Math.max(min, totalHeight - used)
                    : Math.max(min, (int) Math.round((Math.max(0.0001, weights.get(i)) / totalWeight) * totalHeight));
            heights.add(size);
            used += size;
        }
        int diff = totalHeight - used;
        for (int i = heights.size() - 1; i >= 0 && diff != 0; i--) {
            int current = heights.get(i);
            int min = Math.max(1, minHeights.get(i));
            if (diff > 0) {
                heights.set(i, current + diff);
                diff = 0;
            } else {
                int reducible = Math.max(0, current - min);
                int reduce = Math.min(reducible, -diff);
                heights.set(i, current - reduce);
                diff += reduce;
            }
        }
        return heights;
    }

    private void applyOrderedSegmentRatios(List<Object> segments, List<Integer> heights, int totalHeight) {
        double safeTotal = Math.max(1.0, totalHeight);
        for (int i = 0; i < segments.size(); i++) {
            Object segment = segments.get(i);
            double ratio = Math.max(0.0001, heights.get(i) / safeTotal);
            if (segment == null) {
                mainPaneHeightRatio = ratio;
                mainOrderIndex = i;
            } else {
                ((ChartPane) segment).setHeightRatio(ratio);
            }
        }
        List<ChartPane> stacked = new ArrayList<>();
        for (Object segment : segments) {
            if (segment instanceof ChartPane pane) {
                stacked.add(pane);
            }
        }
        syncMainPaneRatio(stacked);
    }

    private ParamsBox computeSharedBox() {
        boolean showSharedY = shouldReserveSharedYAxis();
        boolean showSharedX = shouldReserveSharedXAxis();
        int reservedY = showSharedY ? params.getYAxisLabelWidthPx() + params.getAxisLabelGapPx() : 0;
        int reservedTop = showSharedX && axisXLocation == AxisXLocation.TOP ? params.getXAxisLabelHeightPx() + params.getAxisLabelGapPx() : 0;
        int reservedBottom = showSharedX && axisXLocation == AxisXLocation.BOTTOM ? params.getXAxisLabelHeightPx() + params.getAxisLabelGapPx() : 0;
        int left = params.getContentPadLeft() + (axisYLocation == AxisYLocation.LEFT ? reservedY : 0);
        int right = getWidth() - params.getContentPadRight() - (axisYLocation == AxisYLocation.RIGHT ? reservedY : 0);
        int top = params.getContentPadTop() + reservedTop;
        int bottom = getHeight() - params.getContentPadBottom() - reservedBottom;
        return new ParamsBox(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private boolean shouldReserveSharedYAxis() {
        return axisYLocation != AxisYLocation.NONE && getWidth() >= MIN_AXIS_Y_VISIBLE_WIDTH_PX;
    }

    private boolean shouldReserveSharedXAxis() {
        return axisXLocation != AxisXLocation.NONE && getHeight() >= MIN_AXIS_X_VISIBLE_HEIGHT_PX;
    }

    private AxisYLocation axisYLocationFor(PaneLayout layout) {
        if (axisYLocation == AxisYLocation.NONE) {
            return AxisYLocation.NONE;
        }
        if (layout != null && layout.pane() != null && layout.pane().getLocation() == PaneLocation.OVERLAY_BOTTOM) {
            return AxisYLocation.NONE;
        }
        Rectangle bounds = layout == null ? null : layout.bounds();
        Rectangle content = layout == null ? null : layout.contentBounds();
        int width = bounds == null ? getWidth() : bounds.width;
        int contentWidth = content == null ? width : content.width;
        if (width < MIN_AXIS_Y_VISIBLE_WIDTH_PX || contentWidth < MIN_AXIS_Y_VISIBLE_CONTENT_WIDTH_PX) {
            return AxisYLocation.NONE;
        }
        return axisYLocation;
    }

    private boolean isXAxisInteractionZone(PaneLayout layout, int x, int y) {
        if (layout == null || layout.overlay() || axisXLocationFor(layout) == AxisXLocation.NONE) {
            return false;
        }
        Rectangle bounds = layout.bounds();
        int axisHeight = params.getXAxisLabelHeightPx() + params.getAxisLabelGapPx() + 6;
        return x >= bounds.x && x <= bounds.x + bounds.width
                && ((axisXLocationFor(layout) == AxisXLocation.BOTTOM && y >= bounds.y + bounds.height - axisHeight)
                || (axisXLocationFor(layout) == AxisXLocation.TOP && y <= bounds.y + axisHeight));
    }

    private boolean isYAxisInteractionZone(PaneLayout layout, int x, int y) {
        AxisYLocation paneAxis = axisYLocationFor(layout);
        if (layout == null || layout.overlay() || paneAxis == AxisYLocation.NONE) {
            return false;
        }
        Rectangle bounds = layout.bounds();
        int axisWidth = params.getYAxisLabelWidthPx() + params.getAxisLabelGapPx() + 8;
        return y >= bounds.y && y <= bounds.y + bounds.height
                && ((paneAxis == AxisYLocation.RIGHT && x >= bounds.x + bounds.width - axisWidth)
                || (paneAxis == AxisYLocation.LEFT && x <= bounds.x + axisWidth));
    }

    private boolean isContentInteractionZone(PaneLayout layout, int x, int y) {
        if (layout == null) {
            return false;
        }
        Rectangle content = layout.contentBounds();
        return content != null && content.contains(x, y);
    }

    private void paintStackedAxisBackgrounds(Graphics2D graphics) {
        if (!shouldReserveSharedYAxis() || axisYLocation == AxisYLocation.NONE) {
            return;
        }
        int axisBandWidth = params.getYAxisLabelWidthPx() + params.getAxisLabelGapPx();
        for (PaneLayout layout : paneLayouts) {
            if (layout.overlay()) {
                continue;
            }
            Rectangle bounds = layout.bounds();
            Color topColor = layout.main() ? new Color(14, 20, 32, 235) : new Color(18, 26, 40, 235);
            Color bottomColor = layout.main() ? new Color(8, 12, 22, 245) : new Color(10, 16, 28, 245);
            int axisX = axisYLocation == AxisYLocation.RIGHT
                    ? Math.max(0, getWidth() - params.getContentPadRight() - axisBandWidth)
                    : params.getContentPadLeft();
            graphics.setPaint(new GradientPaint(axisX, bounds.y, topColor, axisX, bounds.y + bounds.height, bottomColor));
            graphics.fillRect(axisX, bounds.y, axisBandWidth, bounds.height);
        }
    }

    private void paintPane(PaneLayout layout) {
        AxisXLocation paneXAxis = axisXLocationFor(layout);
        AxisYLocation paneYAxis = axisYLocationFor(layout);
        ChartEngine.Params paneParams = new ChartEngine.Params()
                .setViewWidthPx(getWidth())
                .setViewHeightPx(getHeight())
                .setDrawAxisLabels(true)
                .setAxisXLocation(paneXAxis)
                .setAxisYLocation(paneYAxis)
                .setXAxisLabelFormatter(this::formatXAxisWorldValue)
                .setXAxisLabelProvider(this::buildXAxisLabel)
                .setDrawCrosshair(false)
                .setCrosshairScreenX(mouseX)
                .setCrosshairScreenY(mouseY);

        ChartEngine.ChartRect outerRect = new ChartEngine.ChartRect(layout.bounds().x, layout.bounds().y, layout.bounds().width, layout.bounds().height);
        Rectangle contentBounds = layout.contentBounds();


        if (layout.overlay()) {
            renderList.add(new RenderCommand.RoundedRect(outerRect.left() - 1f, outerRect.top() - 1f, outerRect.right() + 1f, outerRect.bottom() + 1f, 12f, new Style(0xCC0F172A, 1f, true).withCornerRadius(12f)));
            renderList.add(new RenderCommand.RoundedRect(outerRect.left() - 1f, outerRect.top() - 1f, outerRect.right() + 1f, outerRect.bottom() + 1f, 12f, new Style(ChartStyle.COLOR_PANEL_BORDER, 1f, false).withCornerRadius(12f)));
            renderList.add(new RenderCommand.Text(outerRect.left() + 10f, outerRect.top() + 16f, layout.title(), 11f, new Style(ChartStyle.COLOR_TEXT_MUTED, 1f, false)));
        }

        if (contentBounds.width <= 0 || contentBounds.height <= 0) {
            return;
        }

        ChartEngine.ChartRect rect = new ChartEngine.ChartRect(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
        chartEngine.buildBaseIntoRect(renderList, layout.viewport(), paneParams, rect, false);
        renderList.add(new RenderCommand.ClipPush(rect.left(), rect.top(), rect.right(), rect.bottom()));
        Transform transform = new Transform(rect.left(), rect.top(), rect.width(), rect.height(), layout.viewport());
        int from = (int) Math.floor(viewport.getXMin());
        int to = (int) Math.ceil(viewport.getXMax());

        if (layout.main()) {
            SwingChartMainRenderers.renderMainChart(mainChartType, mainDataSet, renderList, transform, from, to);
            for (DataSet dataSet : mainPaneDataSets()) {
                if (dataSet.isVisible() && dataSet.supports(activeTimeframe)) {
                    if (dataSet instanceof LineDataSet lineDataSet) lineDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof concrete.goonie.chart.api.data.BarDataSet barDataSet) barDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof CandlestickDataSet candlestickDataSet) candlestickDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof LineDataSet lineDataSet) lineDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof concrete.goonie.chart.api.data.BarDataSet barDataSet) barDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof CandlestickDataSet candlestickDataSet) candlestickDataSet.setOptimizationOptions(optimizationOptions);
                    dataSet.render(renderList, transform, from, to);
                }
            }
            for (Tool tool : tools.all()) {
                if (tool.isVisible() && tool.supports(activeTimeframe)) {
                    tool.render(renderList, transform);
                }
            }
            paintTradingOverlays(transform);
            paintReplayOverlayInfo(rect);
        } else if (layout.pane() != null) {
            for (DataSet dataSet : paneDataSets(layout.pane())) {
                if (dataSet.isVisible() && dataSet.supports(activeTimeframe)) {
                    dataSet.render(renderList, transform, from, to);
                }
            }
        }
        renderList.add(new RenderCommand.ClipPop());
    }


    private void paintTradingOverlays(Transform transform) {
        if (Double.isFinite(bidPrice)) {
            addPriceLine(transform, bidPrice, "BID", 0xFF22C55E, false);
        }
        if (Double.isFinite(askPrice)) {
            addPriceLine(transform, askPrice, "ASK", 0xFFEF4444, false);
        }
        double reference = Double.isFinite(bidPrice) ? bidPrice : askPrice;
        for (PositionInfo positionInfo : positions) {
            addPriceLine(transform, positionInfo.getOpenPrice(), positionInfo.getId() + " OP", 0xFFEAB308, true);
            addPriceLine(transform, positionInfo.getStopLoss(), positionInfo.getId() + " SL", 0xFFFB7185, true);
            addPriceLine(transform, positionInfo.getTakeProfit(), positionInfo.getId() + " TP", 0xFF60A5FA, true);
            if (Double.isFinite(reference)) {
                double pips = positionInfo.pipDifferenceTo(reference);
                int y = transform.worldToScreenY(positionInfo.getOpenPrice());
                renderList.add(new RenderCommand.Text(transform.right() - 160f, y - 4f, String.format(Locale.US, "%s %.1f pips", positionInfo.getId(), pips), 11f, new Style(ChartStyle.COLOR_TEXT, 1f, false)));
            }
        }
    }

    private void addPriceLine(Transform transform, double price, String label, int color, boolean dashed) {
        int y = transform.worldToScreenY(price);
        if (y < transform.top() || y > transform.bottom()) {
            return;
        }
        Style lineStyle = new Style(color, 1.4f, false);
        lineStyle.dashed = dashed;
        renderList.add(new RenderCommand.Line(transform.left(), y, transform.right(), y, lineStyle));
        renderList.add(new RenderCommand.RoundedRect(transform.right() - 82f, y - 11f, transform.right() - 6f, y + 9f, 7f, new Style(0xE61E293B, 1f, true).withCornerRadius(7f)));
        renderList.add(new RenderCommand.RoundedRect(transform.right() - 82f, y - 11f, transform.right() - 6f, y + 9f, 7f, new Style(color, 1f, false).withCornerRadius(7f)));
        renderList.add(new RenderCommand.Text(transform.right() - 76f, y + 3f, String.format(Locale.US, "%s %.2f", label, price), 11f, new Style(0xFFF8FAFC, 1f, false)));
    }

    private void paintReplayOverlayInfo(ChartEngine.ChartRect rect) {
        if (!replayState.isOverlayVisible()) {
            return;
        }
        renderList.add(new RenderCommand.RoundedRect(rect.left() + 12f, rect.top() + 12f, rect.left() + 300f, rect.top() + 62f, 10f, new Style(0xCC0B1220, 1f, true).withCornerRadius(10f)));
        renderList.add(new RenderCommand.RoundedRect(rect.left() + 12f, rect.top() + 12f, rect.left() + 300f, rect.top() + 62f, 10f, new Style(0xFF60A5FA, 1f, false).withCornerRadius(10f)));
        String stateText = replayState.isAwaitingStartSelection()
                ? (replayState.isXPanSelectionEnabled() ? "Move cursor or pan X, then click to choose replay start" : "Move cursor, then click a bar to choose replay start")
                : "Replay start: " + formatSourceIndexAsTime(replayState.getSelectedSourceIndex());
        renderList.add(new RenderCommand.Text(rect.left() + 24f, rect.top() + 34f, "Replay / Backtest", 12f, new Style(0xFFF8FAFC, 1f, false)));
        renderList.add(new RenderCommand.Text(rect.left() + 24f, rect.top() + 52f, stateText, 11f, new Style(0xFFCBD5E1, 1f, false)));
    }

    private void paintReplaySelectionOverlay(RenderList out) {
        if (!replayState.isOverlayVisible() || mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
            return;
        }
        Rectangle sharedBounds = getSharedReplayBounds();
        Transform sharedTransform = getSharedReplayTransform(sharedBounds);
        if (sharedBounds == null || sharedTransform == null || sharedBounds.width <= 0 || sharedBounds.height <= 0) {
            return;
        }

        int trackedIndex = currentReplayOverlaySourceIndex();
        if (trackedIndex < 0) {
            return;
        }
        double trackedWorldX = worldXForSourceIndex(trackedIndex, activeTimeframe);
        int trackedX = Math.max(sharedBounds.x, Math.min(sharedBounds.x + sharedBounds.width, sharedTransform.worldToScreenX(trackedWorldX)));
        int overlayRight = sharedBounds.x + sharedBounds.width;

        if (trackedX < overlayRight) {
            out.add(new RenderCommand.RoundedRect(trackedX, sharedBounds.y, overlayRight, sharedBounds.y + sharedBounds.height, 0f, new Style(0x52101727, 1f, true)));
        }
        out.add(new RenderCommand.Line(trackedX, sharedBounds.y, trackedX, sharedBounds.y + sharedBounds.height, new Style(0xFF38BDF8, 4f, false)));
    }

private void paintStackedSplitters(RenderList out) {
    for (int i = 0; i < paneLayouts.size(); i++) {
        PaneLayout layout = paneLayouts.get(i);
        if (layout.main() || layout.overlay()) {
            continue;
        }
        int splitterY = layout.bounds().y;
        boolean active = i == activeSplitterIndex;
        boolean hovered = i == hoveredSplitterIndex;
        int baseColor = 0x2AF8FAFC;
        int lineColor = active ? 0xFF60A5FA : (hovered ? 0xFF93C5FD : 0x66CBD5E1);
        int glowColor = active ? 0x4460A5FA : (hovered ? 0x2293C5FD : 0x00000000);
        int startX = layout.bounds().x;
        int endX = layout.bounds().x + layout.bounds().width;
        for (int x = startX; x < endX; x += 12) {
            int x2 = Math.min(endX, x + 6);
            out.add(new RenderCommand.Line(x, splitterY, x2, splitterY, new Style(baseColor, 1f, false)));
            out.add(new RenderCommand.Line(x, splitterY + 1, x2, splitterY + 1, new Style(0x1438BDF8, 1f, false)));
        }
        if (hovered || active) {
            out.add(new RenderCommand.Line(layout.bounds().x, splitterY, layout.bounds().x + layout.bounds().width, splitterY,
                    new Style(glowColor, active ? 5f : 3f, false)));
        }
        out.add(new RenderCommand.Line(layout.bounds().x, splitterY, layout.bounds().x + layout.bounds().width, splitterY,
                new Style(lineColor, active ? 1.8f : 1.2f, false)));
    }
}

private int findHoveredSplitterIndex(int x, int y) {
    for (int i = 0; i < paneLayouts.size(); i++) {
        PaneLayout layout = paneLayouts.get(i);
        if (layout.main() || layout.overlay()) {
            continue;
        }
        int splitterY = layout.bounds().y;
        if (x >= layout.bounds().x && x <= layout.bounds().x + layout.bounds().width
                && Math.abs(y - splitterY) <= SPLITTER_HIT_HALF_HEIGHT_PX) {
            return i;
        }
    }
    return -1;
}

private void resizeStackedSplitter(int layoutIndex, int deltaY) {
    if (layoutIndex < 0 || paneLayouts.isEmpty()) {
        return;
    }
    List<PaneLayout> ordered = new ArrayList<>();
    List<ChartPane> stacked = new ArrayList<>();
    for (PaneLayout layout : paneLayouts) {
        if (!layout.overlay()) {
            ordered.add(layout);
            if (!layout.main() && layout.pane() != null) {
                stacked.add(layout.pane());
            }
        }
    }
    int orderedIndex = -1;
    for (int i = 0; i < ordered.size(); i++) {
        if (ordered.get(i) == paneLayouts.get(layoutIndex)) {
            orderedIndex = i;
            break;
        }
    }
    int dividerIndex = orderedIndex - 1;
    if (dividerIndex < 0 || dividerIndex >= ordered.size() - 1) {
        return;
    }

    ParamsBox box = computeSharedBox();
    int totalSegmentGap = Math.max(0, ordered.size() - 1) * STACKED_GAP_PX;
    int totalSegmentHeight = Math.max(0, box.contentHeight() - totalSegmentGap);
    List<Integer> minHeights = new ArrayList<>();
    minHeights.add(getMainPaneMinHeightPx());
    for (ChartPane pane : stacked) {
        PaneWindowState state = stateFor(pane);
        minHeights.add(state.collapsed() ? PANE_HEADER_HEIGHT_PX : getStackedPaneMinHeightPx());
    }

    StackedPaneSplitLayout.LayoutSnapshot snapshot = StackedPaneSplitLayout.resize(
            mainPaneHeightRatio,
            stacked,
            dividerIndex,
            deltaY,
            totalSegmentHeight,
            STACKED_GAP_PX,
            minHeights
    );
    mainPaneHeightRatio = snapshot.mainWeight();
    List<Double> paneWeights = snapshot.paneWeights();
    for (int i = 0; i < stacked.size() && i < paneWeights.size(); i++) {
        stacked.get(i).setHeightRatio(paneWeights.get(i));
    }
    syncMainPaneRatio(stacked);
    rebuildPaneLayouts();
    syncPaneViewports();
}

private void syncMainPaneRatio(List<ChartPane> stacked) {
    double stackedSum = 0.0;
    for (ChartPane pane : stacked) {
        stackedSum += Math.max(0.0001, pane.getHeightRatio());
    }
    double total = Math.max(0.0001, mainPaneHeightRatio) + stackedSum;
    mainPaneHeightRatio = Math.max(0.0001, mainPaneHeightRatio) / total;
    for (ChartPane pane : stacked) {
        pane.setHeightRatio(Math.max(0.0001, pane.getHeightRatio()) / total);
    }
}

private AxisXLocation axisXLocationFor(PaneLayout layout) {
        if (axisXLocation == AxisXLocation.NONE) {
            return AxisXLocation.NONE;
        }
        Rectangle bounds = layout == null ? null : layout.bounds();
        int height = bounds == null ? getHeight() : bounds.height;
        return height < MIN_AXIS_X_VISIBLE_HEIGHT_PX ? AxisXLocation.NONE : AxisXLocation.NONE;
    }

    private boolean isCrosshairPane(PaneLayout layout) {
        return mouseX >= 0 && mouseY >= 0 && layout.bounds().contains(mouseX, mouseY);
    }

    private void paintGlobalXLayer(RenderList out) {
        PaneLayout first = null;
        PaneLayout last = null;
        for (PaneLayout layout : paneLayouts) {
            if (layout.overlay()) {
                continue;
            }
            if (first == null) {
                first = layout;
            }
            last = layout;
        }
        if (first == null || last == null || viewport == null) {
            return;
        }

        ParamsBox box = computeSharedBox();
        int left = box.left();
        int right = box.left() + box.width();
        int top = first.bounds().y;
        int bottom = last.bounds().y + last.bounds().height;

        List<concrete.goonie.chart.internal.render.GridLines.Line> xLines = concrete.goonie.chart.internal.render.GridLines.buildX(viewport, box.width(), params.getGridMinorDivisions());
        Style minor = new Style(ChartStyle.COLOR_GRID_MINOR, 1f, false);
        Style major = new Style(ChartStyle.COLOR_GRID_MAJOR, 1f, false);
        for (concrete.goonie.chart.internal.render.GridLines.Line line : xLines) {
            int x = left + (int) Math.round(((line.worldValue - viewport.getXMin()) / Math.max(1e-9, viewport.xRange())) * box.width());
            if (x >= left && x <= right) {
                out.add(new RenderCommand.Line(x, top, x, bottom, line.major ? major : minor));
            }
        }

        if (axisXLocation == AxisXLocation.TOP) {
            out.add(new RenderCommand.Line(left, top, right, top, new Style(ChartStyle.COLOR_AXIS, 1.3f, false)));
        } else if (axisXLocation == AxisXLocation.BOTTOM) {
            out.add(new RenderCommand.Line(left, bottom, right, bottom, new Style(ChartStyle.COLOR_AXIS, 1.3f, false)));
        }

        float labelY = axisXLocation == AxisXLocation.TOP
                ? Math.max(10f, top - params.getAxisLabelGapPx())
                : bottom + params.getLabelTextSize() + params.getAxisLabelGapPx();
        Style textStyle = new Style(ChartStyle.COLOR_TEXT_MUTED, 1f, false);
        for (concrete.goonie.chart.internal.render.GridLines.Line line : xLines) {
            if (!line.major) {
                continue;
            }
            int x = left + (int) Math.round(((line.worldValue - viewport.getXMin()) / Math.max(1e-9, viewport.xRange())) * box.width());
            if (x < left || x > right) {
                continue;
            }
            ChartEngine.AxisLabel axisLabel = buildXAxisLabel(line.worldValue);
            String label = axisLabel == null || axisLabel.text() == null ? "" : axisLabel.text();
            float textSize = axisLabel == null ? params.getLabelTextSize() : axisLabel.textSize();
            int fontStyle = axisLabel == null ? Font.PLAIN : axisLabel.fontStyle();
            out.add(new RenderCommand.Text(x - 20, labelY, label, textSize, fontStyle, textStyle));
        }

        PaneLayout hoveredPane = findPaneLayoutAt(mouseX, mouseY);
        if (hoveredPane == null || hoveredPane.overlay() || mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) {
            return;
        }

        out.add(new RenderCommand.Line(mouseX, top, mouseX, bottom, new Style(ChartStyle.COLOR_CROSS, 1f, false)));
        Rectangle hoveredBounds = hoveredPane.contentBounds().height > 0 ? hoveredPane.contentBounds() : hoveredPane.bounds();
        if (mouseY >= hoveredBounds.y && mouseY <= hoveredBounds.y + hoveredBounds.height) {
            out.add(new RenderCommand.Line(hoveredBounds.x, mouseY, hoveredBounds.x + hoveredBounds.width, mouseY, new Style(ChartStyle.COLOR_CROSS, 1f, false)));
            if (axisYLocationFor(hoveredPane) != AxisYLocation.NONE && hoveredPane.viewport() != null) {
                Transform transform = transformFor(hoveredPane);
                float labelLeft = axisYLocation == AxisYLocation.LEFT
                        ? Math.max(4f, hoveredBounds.x - params.getYAxisLabelWidthPx())
                        : hoveredBounds.x + hoveredBounds.width + 4f;
                out.add(new RenderCommand.RoundedRect(labelLeft, mouseY - 14f, labelLeft + 54f, mouseY + 6f, 7f, new Style(ChartStyle.COLOR_PANEL, 1f, true).withCornerRadius(7f)));
                out.add(new RenderCommand.Text(labelLeft + 8f, mouseY, String.format(Locale.US, "%.2f", transform.screenToWorldY(mouseY)), params.getLabelTextSize(), new Style(ChartStyle.COLOR_TEXT, 1f, false)));
            }
        }
    }

    private void paintTooltip(RenderList out, TooltipInfo info) {
        float boxWidth = Math.max(130f, info.text().length() * 7.2f);
        float boxHeight = 40f;
        float left = Math.min(getWidth() - boxWidth - 12f, info.x() + 12f);
        float top = Math.max(12f, info.y() - 18f);
        out.add(new RenderCommand.RoundedRect(left, top, left + boxWidth, top + boxHeight, 10f, new Style(ChartStyle.COLOR_PANEL, 1f, true).withCornerRadius(10f)));
        out.add(new RenderCommand.RoundedRect(left, top, left + boxWidth, top + boxHeight, 10f, new Style(ChartStyle.COLOR_PANEL_BORDER, 1f, false).withCornerRadius(10f)));
        out.add(new RenderCommand.Circle(info.x(), info.y(), 4f, new Style(ChartStyle.COLOR_MARKER, 1f, true)));
        out.add(new RenderCommand.Text(left + 12f, top + 18f, info.title(), 11f, new Style(ChartStyle.COLOR_TEXT_MUTED, 1f, false)));
        out.add(new RenderCommand.Text(left + 12f, top + 33f, info.text(), 12f, new Style(ChartStyle.COLOR_TEXT, 1f, false)));
    }

    private void installInputHandlers() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                updateHoverState(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                mouseX = event.getX();
                mouseY = event.getY();
                if (!dragging || mainDataSet == null || viewport == null) {
                    return;
                }

                PaneLayout interactionPane = activeInteractionPane == null ? findPaneLayoutAt(mouseX, mouseY) : activeInteractionPane;
                if (interactionPane == null) {
                    return;
                }

                if (resizingStackedPanes && activeSplitterIndex >= 0) {
                    resizeStackedSplitter(activeSplitterIndex, event.getY() - lastY);
                } else {
                    Transform transform = transformFor(interactionPane);
                    if (activeDragTool != null && activeControlPointIndex >= 0 && interactionPane.main()) {
                        double[] snapped = snapWorldPoint(transform, activeDragTool, activeControlPointIndex, transform.screenToWorldX(event.getX()), transform.screenToWorldY(event.getY()));
                        activeDragTool.moveControlPoint(activeControlPointIndex, snapped[0], snapped[1]);
                    } else if (activeDragTool != null && interactionPane.main()) {
                        double dxWorld = transform.screenToWorldX(event.getX()) - transform.screenToWorldX(lastX);
                        double dyWorld = transform.screenToWorldY(event.getY()) - transform.screenToWorldY(lastY);
                        translateSelectedTools(dxWorld, dyWorld);
                    } else {
                        boolean xOnly = isXAxisInteractionZone(interactionPane, lastX, lastY) || isXAxisInteractionZone(interactionPane, event.getX(), event.getY());
                        boolean yOnly = isYAxisInteractionZone(interactionPane, lastX, lastY) || isYAxisInteractionZone(interactionPane, event.getX(), event.getY());
                        panByPixels(interactionPane, event.getX() - lastX, event.getY() - lastY, !yOnly, !xOnly);
                        if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && replayState.isXPanSelectionEnabled()) {
                            replaySelectionPannedThisDrag = true;
                            replayState.setTrackedSourceIndex(clampTrackedSourceIndex(replayState.getTrackedSourceIndex()));
                        }
                    }
                }

                lastX = event.getX();
                lastY = event.getY();
                updateHoverState(event);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                mouseX = event.getX();
                mouseY = event.getY();
                lastX = event.getX();
                lastY = event.getY();
                activeDragTool = null;
                activeControlPointIndex = -1;
                replaySelectionPannedThisDrag = false;
                if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON3) {
                    showPanePopup(event);
                    return;
                }
                HeaderActionHit headerActionHit = findHeaderActionAt(event.getX(), event.getY());
                if (headerActionHit != null) {
                    performHeaderAction(headerActionHit);
                    dragging = false;
                    resizingStackedPanes = false;
                    activeInteractionPane = null;
                    updateHoverState(event);
                    repaint();
                    return;
                }
                activeSplitterIndex = findHoveredSplitterIndex(event.getX(), event.getY());
                resizingStackedPanes = activeSplitterIndex >= 0;
                activeInteractionPane = resizingStackedPanes ? findPaneLayoutAt(event.getX(), event.getY() + STACKED_GAP_PX) : findPaneLayoutAt(event.getX(), event.getY());

                if (activeInteractionPane == null && !resizingStackedPanes) {
                    clearSelection();
                    dragging = false;
                    repaint();
                    return;
                }

                if (resizingStackedPanes) {
                    dragging = true;
                    repaint();
                    return;
                }

                if (!activeInteractionPane.overlay()) {
                    if (tryCreatePendingTool(event, activeInteractionPane)) {
                        dragging = false;
                        updateHoverState(event);
                        return;
                    }
                    if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && !replayState.isXPanSelectionEnabled()) {
                        Transform transform = getSharedReplayTransform(getSharedReplayBounds());
                        if (transform == null) {
                            transform = transformFor(activeInteractionPane);
                        }
                        int selectedIndex = clampSourceIndexForScreenX(event.getX(), transform);
                        selectReplayStartIndex(selectedIndex);
                        dragging = false;
                        updateHoverState(event);
                        repaint();
                        return;
                    }
                    if (activeInteractionPane.main()) {
                        ToolHit toolHit = findToolHit(event.getX(), event.getY(), activeInteractionPane);
                        boolean additiveSelection = event.isControlDown() || event.isShiftDown();
                        if (toolHit != null) {
                            activeDragTool = toolHit.tool();
                            activeControlPointIndex = toolHit.controlPointIndex();
                            if (additiveSelection) {
                                toggleToolSelection(activeDragTool);
                            } else if (!selectedTools.contains(activeDragTool)) {
                                selectOnly(activeDragTool);
                            } else {
                                setSelectedToolInternal(activeDragTool);
                            }
                        } else if (!additiveSelection) {
                            clearSelection();
                        }
                    }
                }
                dragging = true;
                updateHoverState(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showPanePopup(event);
                }
                if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && replayState.isXPanSelectionEnabled()
                        && activeInteractionPane != null && !activeInteractionPane.overlay() && !resizingStackedPanes
                        && !replaySelectionPannedThisDrag && replayState.getTrackedSourceIndex() >= 0) {
                    selectReplayStartIndex(replayState.getTrackedSourceIndex());
                }
                dragging = false;
                resizingStackedPanes = false;
                activeDragTool = null;
                activeControlPointIndex = -1;
                activeSplitterIndex = -1;
                activeInteractionPane = null;
                updateHoverState(event);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                mouseX = -1;
                mouseY = -1;
                hoveredTool = null;
                hoveredControlPointIndex = -1;
                tooltipInfo = null;
                activeInteractionPane = null;
                hoveredHeaderAction = null;
                hoveredSplitterIndex = -1;
                activeSplitterIndex = -1;
                resizingStackedPanes = false;
                updateToolStates(null, null);
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseWheelListener(event -> {
            if (mainDataSet == null || viewport == null) {
                return;
            }
            PaneLayout hovered = findPaneLayoutAt(event.getX(), event.getY());
            if (hovered == null) {
                return;
            }
            double step = 1.12;
            double factor = event.getWheelRotation() > 0 ? step : (1.0 / step);
            boolean xAxisZone = isXAxisInteractionZone(hovered, event.getX(), event.getY());
            boolean yAxisZone = isYAxisInteractionZone(hovered, event.getX(), event.getY());
            boolean zoomX = !yAxisZone;
            boolean zoomY = hovered.main() && (yAxisZone || (!xAxisZone && event.isShiftDown()));
            zoomAt(hovered, event.getX(), event.getY(), factor, zoomX, zoomY);
            updateHoverState(event);
            repaint();
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_R) {
                    resetView();
                } else if (event.getKeyCode() == KeyEvent.VK_F) {
                    fitYNow();
                    repaint();
                } else if (event.getKeyCode() == KeyEvent.VK_DELETE || event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    deleteSelectedTools();
                } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearSelection();
                    setPendingToolType(null);
                    repaint();
                }
            }
        });
    }

    private void panByPixels(PaneLayout pane, int dxPixels, int dyPixels, boolean panX, boolean panY) {
        Transform transform = transformFor(pane);
        if (panX) {
            double dxWorld = -dxPixels / transform.pixelsPerWorldX();
            viewport.setX(viewport.getXMin() + dxWorld, viewport.getXMax() + dxWorld);
            clampX();
        }
        if (panY && pane.main()) {
            double dyWorld = dyPixels / transform.pixelsPerWorldY();
            viewport.setY(viewport.getYMin() + dyWorld, viewport.getYMax() + dyWorld);
        }
        if (autoFitY && panX && !panY) {
            fitYNow();
        } else {
            syncPaneViewports();
        }
    }

    private void zoomAt(PaneLayout pane, int screenX, int screenY, double factor, boolean zoomX, boolean zoomY) {
        Transform transform = transformFor(pane);
        if (zoomX) {
            double anchorX = transform.screenToWorldX(screenX);
            double oldRange = viewport.xRange();
            double maxXRange = Math.max(50, mainDataSet.getSeries().size());
            double newRange = clamp(oldRange * factor, 10, maxXRange);
            double a = (anchorX - viewport.getXMin()) / oldRange;
            viewport.setX(anchorX - a * newRange, anchorX - a * newRange + newRange);
            clampX();
        }
        if (zoomY && pane.main()) {
            double anchorY = transform.screenToWorldY(screenY);
            double oldRange = viewport.yRange();
            double newRange = clamp(oldRange * factor, 0.5, 1e9);
            double a = (anchorY - viewport.getYMin()) / oldRange;
            viewport.setY(anchorY - a * newRange, anchorY - a * newRange + newRange, yAxisRangePolicy);
        }
        if (autoFitY && zoomX && !zoomY) {
            fitYNow();
        } else {
            syncPaneViewports();
        }
    }

    private void clampX() {
        if (viewport == null || mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
            return;
        }
        viewport.applyXPolicy(xAxisRangePolicy);
        double visibleRange = Math.max(1.0, viewport.xRange());
        double pad = Math.max(6.0, visibleRange * 0.50);
        double min = -pad;
        double max = Math.max(1.0, mainDataSet.getSeries().size() - 1) + pad;
        viewport.clampX(min, max);
        viewport.applyXPolicy(xAxisRangePolicy);
    }

    private int clampSourceIndexForViewportRightEdge() {
        if (mainDataSet == null || viewport == null || mainDataSet.getSeries().isEmpty()) {
            return -1;
        }
        return clampSourceIndexForScreenX(getSharedReplayBounds() == null ? getWidth() : getSharedReplayBounds().x + getSharedReplayBounds().width, getSharedReplayTransform(getSharedReplayBounds()));
    }

    private int clampTrackedSourceIndex(int index) {
        int size = realTimeBaseSeries == null ? (mainDataSet == null ? 0 : mainDataSet.getSeries().size()) : realTimeBaseSeries.size();
        if (size <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(size - 1, index));
    }

    private void updateHoverState(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        rebuildPaneLayouts();
        hoveredHeaderAction = findHeaderActionAt(mouseX, mouseY);
        hoveredSplitterIndex = hoveredHeaderAction == null ? findHoveredSplitterIndex(mouseX, mouseY) : -1;
        PaneLayout hoveredPane = hoveredSplitterIndex >= 0 ? findPaneLayoutAt(mouseX, mouseY + STACKED_GAP_PX) : findPaneLayoutAt(mouseX, mouseY);
        if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && hoveredPane != null && !hoveredPane.overlay()) {
            Transform transform = getSharedReplayTransform(getSharedReplayBounds());
            if (transform == null) {
                transform = transformFor(hoveredPane);
            }
            replayState.setTrackedSourceIndex(clampSourceIndexForScreenX(mouseX, transform));
        }
        ToolHit toolHit = hoveredPane != null && hoveredPane.main() ? findToolHit(mouseX, mouseY, hoveredPane) : null;
        hoveredTool = toolHit == null ? null : toolHit.tool();
        hoveredControlPointIndex = toolHit == null ? -1 : toolHit.controlPointIndex();
        updateToolStates(hoveredTool, activeDragTool);
        tooltipInfo = findTooltip(mouseX, mouseY, hoveredPane, toolHit);
        setCursor(resolveCursor(toolHit, hoveredPane));
        repaint();
    }

    private void setSelectedToolInternal(Tool tool) {
        if (selectedTool == tool) {
            return;
        }
        selectedTool = tool;
        for (Consumer<Tool> listener : selectedToolListeners) {
            listener.accept(tool);
        }
    }

    private void selectOnly(Tool tool) {
        selectedTools.clear();
        if (tool != null) {
            selectedTools.add(tool);
        }
        syncToolSelectionFlags();
        setSelectedToolInternal(tool);
    }

    private void toggleToolSelection(Tool tool) {
        if (tool == null) {
            return;
        }
        if (selectedTools.contains(tool)) {
            selectedTools.remove(tool);
            Tool fallback = selectedTools.isEmpty() ? null : selectedTools.stream().reduce((a, b) -> b).orElse(null);
            syncToolSelectionFlags();
            setSelectedToolInternal(fallback);
        } else {
            selectedTools.add(tool);
            syncToolSelectionFlags();
            setSelectedToolInternal(tool);
        }
    }

    private void syncToolSelectionFlags() {
        for (Tool tool : tools.all()) {
            tool.setSelected(selectedTools.contains(tool));
        }
    }

    private void translateSelectedTools(double dxWorld, double dyWorld) {
        if (activeDragTool == null) {
            return;
        }
        if (selectedTools.size() > 1 && selectedTools.contains(activeDragTool) && activeControlPointIndex < 0) {
            for (Tool tool : selectedTools) {
                tool.translate(dxWorld, dyWorld);
            }
        } else {
            activeDragTool.translate(dxWorld, dyWorld);
        }
    }

    private void deleteSelectedTools() {
        if (selectedTools.isEmpty()) {
            return;
        }
        tools.removeAll(selectedTools);
        selectedTools.clear();
        activeDragTool = null;
        hoveredTool = null;
        setSelectedToolInternal(null);
        repaint();
    }

    private boolean tryCreatePendingTool(MouseEvent event, PaneLayout interactionPane) {
        if (pendingToolType == null || interactionPane == null || !interactionPane.main()) {
            return false;
        }
        Transform transform = transformFor(interactionPane);
        if (transform == null) {
            return false;
        }
        double[] snapped = snapWorldPoint(transform, null, -1, transform.screenToWorldX(event.getX()), transform.screenToWorldY(event.getY()));
        double xWorld = snapped[0];
        double yWorld = snapped[1];
        Tool tool = switch (pendingToolType) {
            case "HLINE" -> new concrete.goonie.chart.api.tool.HorizontalLineTool("Horizontal Line", yWorld, activeTimeframe);
            case "TREND" -> new concrete.goonie.chart.api.tool.TrendLineTool("Trend Line", xWorld - 8.0, yWorld + 0.8, xWorld + 8.0, yWorld - 0.8, activeTimeframe);
            case "RANGE" -> new concrete.goonie.chart.api.tool.RangeBoxTool("Range", xWorld - 6.0, yWorld + 1.2, xWorld + 6.0, yWorld - 1.2, activeTimeframe);
            default -> null;
        };
        if (tool == null) {
            return false;
        }
        tools.add(tool);
        selectOnly(tool);
        pendingToolType = null;
        repaint();
        return true;
    }

    private Cursor resolveCursor(ToolHit toolHit, PaneLayout hoveredPane) {
        if (hoveredSplitterIndex >= 0 || activeSplitterIndex >= 0) {
            return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        }
        if (toolHit != null) {
            return Cursor.getPredefinedCursor(toolHit.controlPointIndex() >= 0 ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR);
        }
        if (hoveredPane == null) {
            return Cursor.getDefaultCursor();
        }
        return Cursor.getPredefinedCursor(hoveredPane.main() ? Cursor.CROSSHAIR_CURSOR : Cursor.E_RESIZE_CURSOR);
    }

    private void updateToolStates(Tool hovered, Tool active) {
        for (Tool tool : tools.all()) {
            tool.setHovered(tool == hovered);
            if (active == null && tool != hovered && !tool.isSelected()) {
                tool.setSelected(false);
            }
        }
    }

    private void clearSelection() {
        selectedTools.clear();
        syncToolSelectionFlags();
        setSelectedToolInternal(null);
    }

    private TooltipInfo findTooltip(int x, int y, PaneLayout paneLayout, ToolHit toolHit) {
        if (paneLayout == null) {
            return null;
        }
        Transform transform = transformFor(paneLayout);

        if (toolHit != null && paneLayout.main()) {
            if (toolHit.controlPointIndex() >= 0) {
                Tool.ControlPoint point = toolHit.tool().controlPoints().get(toolHit.controlPointIndex());
                int sx = transform.worldToScreenX(point.xWorld());
                int sy = transform.worldToScreenY(point.yWorld());
                return new TooltipInfo(sx, sy, toolHit.tool().getName(), point.role() + "  t " + formatXAxisWorldValue(point.xWorld()) + "  y " + String.format(Locale.US, "%.2f", point.yWorld()));
            }
            return new TooltipInfo(x, y, "Object", toolHit.tool().tooltip());
        }

        if (paneLayout.main()) {
            int candleIndex = clampMainSeriesIndexForWorldX(transform.screenToWorldX(x));
            Candle candle = mainDataSet.getSeries().get(candleIndex);
            int candleX = transform.worldToScreenX(candleIndex);
            int candleY = transform.worldToScreenY(candle.close());
            if (Math.abs(candleX - x) <= 9) {
                String text = String.format(Locale.US, "%s  O %.2f H %.2f L %.2f C %.2f",
                        candle.time().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        candle.open(), candle.high(), candle.low(), candle.close());
                return new TooltipInfo(candleX, candleY, "Candle", text);
            }
        }

        TooltipInfo dataTip = findDataSetTooltip(transform, x, y, paneLayout);
        if (dataTip != null) {
            return dataTip;
        }
        return new TooltipInfo(x, y, paneLayout.title(), String.format(Locale.US, "%s  Y %.2f", formatXAxisWorldValue(transform.screenToWorldX(x)), transform.screenToWorldY(y)));
    }

    private TooltipInfo findDataSetTooltip(Transform transform, int x, int y, PaneLayout paneLayout) {
        List<DataSet> allDataSets = paneLayout.main() ? mainPaneDataSets() : paneDataSets(paneLayout.pane());
        TooltipInfo nearest = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (DataSet dataSet : allDataSets) {
            if (!(dataSet instanceof ValueDataSet valueDataSet) || !dataSet.supports(activeTimeframe)) {
                continue;
            }
            for (int i = 0; i < valueDataSet.size(); i++) {
                double wx = valueDataSet.xAt(i);
                if (wx < viewport.getXMin() || wx > viewport.getXMax()) {
                    continue;
                }
                double wy = valueDataSet.yAt(i);
                if (Double.isNaN(wy)) {
                    continue;
                }
                int sx = transform.worldToScreenX(wx);
                int sy = transform.worldToScreenY(wy);
                double dist = Math.hypot(x - sx, y - sy);
                if (dist <= 12.0 && dist < bestDist) {
                    bestDist = dist;
                    nearest = new TooltipInfo(sx, sy, dataSet.getName(), String.format(Locale.US, "x %.0f  y %.2f", wx, wy));
                }
            }
        }
        return nearest;
    }

    private ToolHit findToolHit(int x, int y, PaneLayout paneLayout) {
        if (paneLayout == null || !paneLayout.main()) {
            return null;
        }
        Transform transform = transformFor(paneLayout);
        List<Tool> renderable = tools.renderableFor(activeTimeframe);
        ToolHit best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int i = renderable.size() - 1; i >= 0; i--) {
            Tool tool = renderable.get(i);
            if (!tool.isVisible()) {
                continue;
            }
            double radius = tool.controlPointHitRadiusPx();
            int controlPointIndex = tool.hitTestControlPoint(transform, x, y, radius);
            if (controlPointIndex >= 0) {
                double score = 0.001 + i * 1e-6;
                if (score < bestScore) {
                    bestScore = score;
                    best = new ToolHit(tool, controlPointIndex);
                }
                continue;
            }
            double distance = tool.hitTestDistancePx(transform, x, y);
            if (distance <= 7.0) {
                double score = distance + i * 1e-6;
                if (score < bestScore) {
                    bestScore = score;
                    best = new ToolHit(tool, -1);
                }
            }
        }
        return best;
    }

    private PaneLayout findPaneLayoutAt(int x, int y) {
        for (int i = paneLayouts.size() - 1; i >= 0; i--) {
            PaneLayout layout = paneLayouts.get(i);
            if (layout.bounds().contains(x, y)) {
                return layout;
            }
        }
        return null;
    }

    private Transform transformFor(PaneLayout layout) {
        Rectangle bounds = layout.contentBounds().height > 0 ? layout.contentBounds() : layout.bounds();
        return new Transform(bounds.x, bounds.y, bounds.width, bounds.height, layout.viewport());
    }

    private Rectangle getSharedReplayBounds() {
        Rectangle shared = null;
        for (PaneLayout layout : paneLayouts) {
            if (layout.overlay()) {
                continue;
            }
            Rectangle bounds = layout.contentBounds().height > 0 ? layout.contentBounds() : layout.bounds();
            if (bounds.width <= 0 || bounds.height <= 0) {
                continue;
            }
            if (shared == null) {
                shared = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            } else {
                int top = Math.min(shared.y, bounds.y);
                int bottom = Math.max(shared.y + shared.height, bounds.y + bounds.height);
                shared = new Rectangle(bounds.x, top, bounds.width, bottom - top);
            }
        }
        return shared;
    }

    private Transform getSharedReplayTransform(Rectangle sharedBounds) {
        if (sharedBounds == null || viewport == null) {
            return null;
        }
        return new Transform(sharedBounds.x, sharedBounds.y, sharedBounds.width, sharedBounds.height, viewport);
    }


    private List<DataSet> mainPaneDataSets() {
        List<DataSet> out = new ArrayList<>(customDataSets.all());
        outer: for (Indicator indicator : indicators.renderableFor(activeTimeframe)) {
            for (ChartPane pane : panes) {
                if (pane.getIndicators().contains(indicator)) {
                    continue outer;
                }
            }
            out.addAll(indicator.getRenderableDataSets(activeTimeframe));
        }
        return out;
    }

    private List<DataSet> paneDataSets(ChartPane pane) {
        if (pane == null) {
            return List.of();
        }
        List<DataSet> out = new ArrayList<>(pane.getDataSets());
        for (Indicator indicator : pane.getIndicators()) {
            out.addAll(indicator.getRenderableDataSets(activeTimeframe));
        }
        return out;
    }

    private Range extractRange(DataSet dataSet, int from, int to) {
        if (!(dataSet instanceof ValueDataSet valueDataSet)) {
            return null;
        }
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < valueDataSet.size(); i++) {
            double x = valueDataSet.xAt(i);
            if (x < from || x > to) {
                continue;
            }
            double y = valueDataSet.yAt(i);
            if (Double.isNaN(y)) {
                continue;
            }
            low = Math.min(low, y);
            high = Math.max(high, y);
        }
        if (!Double.isFinite(low) || !Double.isFinite(high)) {
            return null;
        }
        return new Range(low, high);
    }

    private static Viewport createDefaultViewport(CandleSeries series) {
        int count = Math.max(1, series.size());
        double visibleBars = Math.min(200.0, Math.max(30.0, count));
        double rightPad = Math.max(8.0, visibleBars * 0.10);
        double xMax = Math.max(1.0, (count - 1) + rightPad);
        double xMin = xMax - visibleBars;
        int from = Math.max(0, count - 1 - (int) Math.ceil(visibleBars));
        int to = Math.max(0, count - 1);
        double yMin = series.minLow(from, to);
        double yMax = series.maxHigh(from, to);
        double pad = Math.max(1e-9, (yMax - yMin) * 0.10);
        return new Viewport(xMin, xMax, yMin - pad, yMax + pad);
    }

    private static Viewport createViewportForSeries(CandleSeries newSeries,
                                                    CandleSeries previousSeries,
                                                    Viewport previousViewport,
                                                    Timeframe previousTimeframe,
                                                    Timeframe newTimeframe) {
        Viewport fallback = createDefaultViewport(newSeries);
        if (newSeries == null || newSeries.isEmpty() || previousSeries == null || previousSeries.isEmpty() || previousViewport == null) {
            return fallback;
        }

        double previousRangeBars = Math.max(1.0, previousViewport.xRange());
        double previousCenterX = previousViewport.getXMin() + previousRangeBars * 0.5;
        int previousCenterIndex = Math.max(0, Math.min(previousSeries.size() - 1, (int) Math.round(previousCenterX)));
        LocalDateTime centerTime = previousSeries.get(previousCenterIndex).time();

        long previousBarSeconds = Math.max(1L, previousTimeframe.duration().getSeconds());
        long newBarSeconds = Math.max(1L, newTimeframe.duration().getSeconds());
        double visibleSeconds = previousRangeBars * previousBarSeconds;
        double newRangeBars = Math.max(1.0, visibleSeconds / newBarSeconds);
        int newCenterIndex = newSeries.indexAtOrBefore(centerTime);
        if (newCenterIndex < 0) {
            return fallback;
        }

        double xMin = newCenterIndex - newRangeBars * 0.5;
        double xMax = xMin + newRangeBars;
        int from = Math.max(0, (int) Math.floor(xMin));
        int to = Math.min(newSeries.size() - 1, (int) Math.ceil(xMax));
        if (to < from) {
            return fallback;
        }
        double yMin = newSeries.minLow(from, to);
        double yMax = newSeries.maxHigh(from, to);
        double yPad = Math.max(1e-9, (yMax - yMin) * 0.10);
        return new Viewport(xMin, xMax, yMin - yPad, yMax + yPad);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateBidAskFromLastCandle(CandleSeries series) {
        Candle last = series == null ? null : series.last();
        if (last == null) {
            bidPrice = Double.NaN;
            askPrice = Double.NaN;
            return;
        }
        pipSize = 0.01;
        bidPrice = last.close();
        askPrice = bidPrice + last.spread() * pipSize;
    }

    public void setBidAsk(double bidPrice, double askPrice, double pipSize) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.pipSize = pipSize <= 0.0 ? this.pipSize : pipSize;
        repaint();
    }

    public void addPosition(PositionInfo positionInfo) {
        if (positionInfo != null) {
            positions.add(positionInfo);
            repaint();
        }
    }

    public List<PositionInfo> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public ReplayState getReplayState() {
        return replayState;
    }

    public RunMode getRunMode() {
        return runMode;
    }

    public void openReplayOverlay() {
        runMode = RunMode.BACKTEST_REPLAY;
        replayState.setOverlayVisible(true);
        replayState.setAwaitingStartSelection(true);
        replayState.setPlaying(false);
        replayTimer.stop();
        replayState.setTrackedSourceIndex(replayState.getSelectedSourceIndex() >= 0 ? replayState.getSelectedSourceIndex() : clampSourceIndexForViewportRightEdge());
        repaint();
    }

    public void setReplaySelectionXPanEnabled(boolean enabled) {
        replayState.setXPanSelectionEnabled(enabled);
        repaint();
    }

    public void setReplayStartMode(ReplayStartMode replayStartMode) {
        replayState.setStartMode(replayStartMode);
        if (replayStartMode == ReplayStartMode.FIRST_DATE) {
            selectReplayStartIndex(0);
        } else if (replayStartMode == ReplayStartMode.RANDOM && realTimeBaseSeries != null && realTimeBaseSeries.size() > 10) {
            int randomIndex = 5 + new java.util.Random().nextInt(Math.max(1, realTimeBaseSeries.size() - 10));
            selectReplayStartIndex(randomIndex);
        }
    }

    public void setReplayStartDate(LocalDateTime dateTime) {
        replayState.setSelectedStartDate(dateTime);
        if (dateTime != null && realTimeBaseSeries != null) {
            int index = Math.max(0, realTimeBaseSeries.indexAtOrBefore(dateTime));
            selectReplayStartIndex(index);
        }
    }

    public void setReplayInterval(Timeframe timeframe) {
        if (timeframe == Timeframe.M1 || timeframe == Timeframe.M5 || timeframe == Timeframe.M15) {
            replayState.setInterval(timeframe);
            replayTimer.setDelay((int) Math.max(60, 500 / replayState.getSpeed()));
        }
    }

    public void setReplaySpeed(int speed) {
        replayState.setSpeed(speed);
        replayTimer.setDelay((int) Math.max(60, 500 / replayState.getSpeed()));
    }

    public void replayPlay() {
        if (replayState.getCurrentSourceIndex() < 0 && replayState.getSelectedSourceIndex() >= 0) {
            replayState.setCurrentSourceIndex(replayState.getSelectedSourceIndex());
        }
        replayState.setPlaying(true);
        replayTimer.start();
    }

    public void replayPause() {
        replayState.setPlaying(false);
        replayTimer.stop();
    }

    public void replayRestart() {
        if (replayState.getSelectedSourceIndex() >= 0) {
            selectReplayStartIndex(replayState.getSelectedSourceIndex());
        }
    }

    public void replayForward() {
        if (runMode != RunMode.BACKTEST_REPLAY || realTimeBaseSeries == null || realTimeBaseSeries.isEmpty()) {
            return;
        }
        int step = switch (replayState.getInterval()) {
            case M1 -> 1;
            case M5 -> 5;
            case M15 -> 15;
            default -> 1;
        };
        int next = replayState.getCurrentSourceIndex() < 0 ? replayState.getSelectedSourceIndex() : replayState.getCurrentSourceIndex() + step * replayState.getSpeed();
        next = Math.min(realTimeBaseSeries.size() - 1, Math.max(0, next));
        replayState.setCurrentSourceIndex(next);
        currentBaseSeries = realTimeBaseSeries.copyRange(0, next);
        loadMainDataSet(activeSymbol, activeTimeframe);
        if (next >= realTimeBaseSeries.size() - 1) {
            replayPause();
        }
    }

    public void jumpToRealTime() {
        replayPause();
        replayState.setOverlayVisible(false);
        replayState.setAwaitingStartSelection(false);
        runMode = RunMode.REALTIME;
        CandleSeries previousSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        Timeframe previousTimeframe = activeTimeframe;
        Viewport previousViewport = viewport == null ? null : new Viewport(viewport.getXMin(), viewport.getXMax(), viewport.getYMin(), viewport.getYMax());
        currentBaseSeries = realTimeBaseSeries;
        loadMainDataSet(activeSymbol, activeTimeframe);
        CandleSeries liveSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        if (previousSeries != null && previousViewport != null && liveSeries != null && !previousSeries.isEmpty() && !liveSeries.isEmpty()) {
            LocalDateTime previousLastTime = previousSeries.last() == null ? null : previousSeries.last().time();
            LocalDateTime liveLastTime = liveSeries.last() == null ? null : liveSeries.last().time();
            if (previousLastTime != null && liveLastTime != null) {
                double previousLastX = worldXForSeriesTime(previousSeries, previousTimeframe, previousLastTime);
                double liveLastX = worldXForSeriesTime(liveSeries, activeTimeframe, liveLastTime);
                double translateBars = liveLastX - previousLastX;
                viewport.setX(previousViewport.getXMin() + translateBars, previousViewport.getXMax() + translateBars, xAxisRangePolicy);
                clampX();
                fitYNow();
            }
        }
        repaint();
    }

    private void selectReplayStartIndex(int index) {
        if (realTimeBaseSeries == null || realTimeBaseSeries.isEmpty()) {
            return;
        }
        int safe = Math.max(0, Math.min(realTimeBaseSeries.size() - 1, index));
        replayState.setSelectedSourceIndex(safe);
        replayState.setCurrentSourceIndex(safe);
        replayState.setTrackedSourceIndex(safe);
        replayState.setAwaitingStartSelection(false);
        replayState.setOverlayVisible(false);
        currentBaseSeries = realTimeBaseSeries.copyRange(0, safe);
        loadMainDataSet(activeSymbol, activeTimeframe);
        double startWorldX = worldXForSourceIndex(safe, activeTimeframe);
        double range = Math.max(1.0, viewport.xRange());
        viewport.setX(startWorldX - range * 0.85, startWorldX + Math.max(6.0, range * 0.15), xAxisRangePolicy);
        clampX();
        fitYNow();
        repaint();
    }


    public BufferedImage createSnapshotImage() {
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public void saveSnapshot(File file) throws java.io.IOException {
        if (file == null) {
            return;
        }
        File target = file.getName().toLowerCase(Locale.ROOT).endsWith(".png") ? file : new File(file.getParentFile() == null ? new File(".") : file.getParentFile(), file.getName() + ".png");
        ImageIO.write(createSnapshotImage(), "png", target);
    }

    public void copySnapshotToClipboard() {
        BufferedImage image = createSnapshotImage();
        Transferable transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return image;
            }
        };
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, (clipboard, contents) -> { });
    }

    public List<String> describeVisibleLayers() {
        List<String> out = new ArrayList<>();
        out.add(mainDataSet.getName());
        for (DataSet dataSet : customDataSets.all()) {
            out.add(dataSet.getName());
        }
        for (Indicator indicator : indicators.all()) {
            out.add(indicator.getName());
        }
        for (ChartPane pane : panes) {
            out.add("Pane: " + pane.getTitle() + " (" + pane.getLocation() + ")");
        }
        for (Tool tool : tools.all()) {
            out.add(tool.getName());
        }
        out.add("Mode: " + runMode);
        out.add("Positions: " + positions.size());
        return out;
    }


    private String formatXAxisWorldValue(double xWorld) {
        LocalDateTime current = timeAtMainWorldX(xWorld);
        if (current == null) {
            return String.format(Locale.US, "%.0f", xWorld);
        }
        LocalDateTime previous = findPreviousAxisLabelTime(xWorld);
        return getLabel(current, previous);
    }

    private ChartEngine.AxisLabel buildXAxisLabel(double xWorld) {
        LocalDateTime current = timeAtMainWorldX(xWorld);
        if (current == null) {
            return new ChartEngine.AxisLabel(String.format(Locale.US, "%.0f", xWorld), 12f, Font.PLAIN);
        }
        LocalDateTime previous = findPreviousAxisLabelTime(xWorld);
        Font font = getLabelFont(current, previous);
        return new ChartEngine.AxisLabel(getLabel(current, previous), font.getSize2D(), font.getStyle());
    }

    private LocalDateTime findPreviousAxisLabelTime(double xWorld) {
        if (viewport == null) {
            return null;
        }
        List<concrete.goonie.chart.internal.render.GridLines.Line> lines = concrete.goonie.chart.internal.render.GridLines.buildX(viewport, getWidth(), 1);
        double previousWorldX = Double.NaN;
        for (concrete.goonie.chart.internal.render.GridLines.Line line : lines) {
            if (!line.major) {
                continue;
            }
            if (line.worldValue < xWorld - 1e-9) {
                previousWorldX = line.worldValue;
            } else {
                break;
            }
        }
        if (!Double.isFinite(previousWorldX)) {
            return null;
        }
        return timeAtMainWorldX(previousWorldX);
    }

    /**
     * Generates a human-readable label for the axis grid line based on time progression.
     */
    private String getLabel(LocalDateTime current, LocalDateTime previous) {
        if (previous == null || current.getYear() != previous.getYear()) {
            return String.valueOf(current.getYear());
        } else if (current.getMonth() != previous.getMonth()) {
            return MONTH_ABBREV.get(current.getMonthValue());
        } else if (current.getDayOfMonth() != previous.getDayOfMonth()) {
            return String.valueOf(current.getDayOfMonth());
        } else {
            switch (activeTimeframe) {
                case M1:
                case M5:
                case M15:
                case M30:
                    return String.format("%02d:%02d", current.getHour(), current.getMinute());
                case H1:
                case H4:
                    return String.format("%02d:00", current.getHour());
                case D1:
                    return current.getDayOfMonth() + " " + MONTH_ABBREV.get(current.getMonthValue());
                default:
                    return String.valueOf(current.getHour());
            }
        }
    }

    /**
     * Determines the font style used for each label based on its time significance.
     */
    private Font getLabelFont(LocalDateTime current, LocalDateTime previous) {
        if (previous == null || current.getYear() != previous.getYear()) {
            return getFont().deriveFont(Font.BOLD, 15f);
        } else if (current.getMonth() != previous.getMonth()) {
            return getFont().deriveFont(Font.BOLD, 14f);
        } else if (current.getDayOfMonth() != previous.getDayOfMonth()) {
            return getFont().deriveFont(Font.BOLD, 14f);
        } else {
            return getFont().deriveFont(Font.PLAIN, 12f);
        }
    }


    private LocalDateTime timeAtMainWorldX(double xWorld) {
        CandleSeries series = mainDataSet == null ? null : mainDataSet.getSeries();
        return timeAtSeriesWorldX(series, activeTimeframe, xWorld);
    }

    private LocalDateTime timeAtSeriesWorldX(CandleSeries series, Timeframe timeframe, double xWorld) {
        if (series == null || series.isEmpty()) {
            return null;
        }
        long barSeconds = Math.max(1L, timeframe == null ? activeTimeframe.seconds() : timeframe.seconds());
        long offsetBars = Math.round(xWorld);
        return series.get(0).time().plusSeconds(offsetBars * barSeconds);
    }

    private double worldXForSeriesTime(CandleSeries series, Timeframe timeframe, LocalDateTime time) {
        if (series == null || series.isEmpty() || time == null) {
            return 0.0;
        }
        long barSeconds = Math.max(1L, timeframe == null ? activeTimeframe.seconds() : timeframe.seconds());
        long deltaSeconds = Duration.between(series.get(0).time(), time).getSeconds();
        return deltaSeconds / (double) barSeconds;
    }

    private String formatSourceIndexAsTime(int sourceIndex) {
        LocalDateTime dateTime = timeAtSeriesWorldX(realTimeBaseSeries, Timeframe.M1, sourceIndex);
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private int clampMainSeriesIndexForWorldX(double xWorld) {
        CandleSeries series = mainDataSet == null ? null : mainDataSet.getSeries();
        if (series == null || series.isEmpty()) {
            return 0;
        }
        int index = (int) Math.round(xWorld);
        return Math.max(0, Math.min(series.size() - 1, index));
    }

    private int clampSourceIndexForScreenX(int screenX, Transform transform) {
        if (realTimeBaseSeries == null || realTimeBaseSeries.isEmpty()) {
            return 0;
        }
        if (transform == null) {
            return 0;
        }
        LocalDateTime time = timeAtMainWorldX(transform.screenToWorldX(screenX));
        if (time == null) {
            return 0;
        }
        long baseSeconds = Math.max(1L, Timeframe.M1.seconds());
        long deltaSeconds = Duration.between(realTimeBaseSeries.get(0).time(), time).getSeconds();
        int index = (int) Math.round(deltaSeconds / (double) baseSeconds);
        return Math.max(0, Math.min(realTimeBaseSeries.size() - 1, index));
    }

    private void remapToolsToSeries(CandleSeries oldSeries, CandleSeries newSeries, Timeframe oldTimeframe, Timeframe newTimeframe) {
        if (oldSeries == null || newSeries == null || oldSeries.isEmpty() || newSeries.isEmpty()) {
            return;
        }
        for (Tool tool : tools.all()) {
            List<Tool.ControlPoint> points = tool.controlPoints();
            for (int i = 0; i < points.size(); i++) {
                Tool.ControlPoint point = points.get(i);
                LocalDateTime time = timeAtSeriesWorldX(oldSeries, oldTimeframe, point.xWorld());
                if (time == null) {
                    continue;
                }
                double newWorldX = worldXForSeriesTime(newSeries, newTimeframe, time);
                tool.moveControlPoint(i, newWorldX, point.yWorld());
            }
        }
    }

    private double[] snapWorldPoint(Transform transform, Tool movingTool, int controlPointIndex, double xWorld, double yWorld) {
        if (!magnetSnappingEnabled || transform == null) {
            return new double[]{xWorld, yWorld};
        }
        double snappedX = xWorld;
        double snappedY = yWorld;
        double bestXDistancePx = 10.0;
        double bestYDistancePx = 10.0;

        CandleSeries series = mainDataSet == null ? null : mainDataSet.getSeries();
        if (series != null && !series.isEmpty()) {
            int index = Math.max(0, Math.min(series.size() - 1, (int) Math.round(xWorld)));
            Candle candle = series.get(index);
            double[] yCandidates = {candle.open(), candle.high(), candle.low(), candle.close()};
            double candleXDistancePx = Math.abs(transform.worldToScreenX(index) - transform.worldToScreenX(xWorld));
            if (candleXDistancePx <= bestXDistancePx) {
                snappedX = index;
                bestXDistancePx = candleXDistancePx;
            }
            for (double candidateY : yCandidates) {
                double distancePx = Math.abs(transform.worldToScreenY(candidateY) - transform.worldToScreenY(yWorld));
                if (distancePx <= bestYDistancePx) {
                    snappedY = candidateY;
                    bestYDistancePx = distancePx;
                }
            }
        }

        for (Tool tool : tools.all()) {
            if (tool == movingTool || !tool.isVisible() || !tool.supports(activeTimeframe)) {
                continue;
            }
            List<Tool.ControlPoint> points = tool.controlPoints();
            for (int i = 0; i < points.size(); i++) {
                if (tool == movingTool && i == controlPointIndex) {
                    continue;
                }
                Tool.ControlPoint point = points.get(i);
                double xDistancePx = Math.abs(transform.worldToScreenX(point.xWorld()) - transform.worldToScreenX(xWorld));
                double yDistancePx = Math.abs(transform.worldToScreenY(point.yWorld()) - transform.worldToScreenY(yWorld));
                if (xDistancePx <= bestXDistancePx) {
                    snappedX = point.xWorld();
                    bestXDistancePx = xDistancePx;
                }
                if (yDistancePx <= bestYDistancePx) {
                    snappedY = point.yWorld();
                    bestYDistancePx = yDistancePx;
                }
            }
        }
        return new double[]{snappedX, snappedY};
    }

    private void showPanePopup(MouseEvent event) {
        PaneLayout layout = findPaneLayoutAt(event.getX(), event.getY());
        if (layout == null || layout.main()) {
            return;
        }
        JPopupMenu popup = buildPanePopup(layout);
        popup.show(this, event.getX(), event.getY());
    }

    public void translateViewportByNewBarIndex() {
        if (realTimeBaseSeries == null || realTimeBaseSeries.isEmpty()) {
            return;
        }
        CandleSeries previousSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        Timeframe previousTimeframe = activeTimeframe;
        Viewport previousViewport = viewport == null ? null : new Viewport(viewport.getXMin(), viewport.getXMax(), viewport.getYMin(), viewport.getYMax());
        currentBaseSeries = realTimeBaseSeries;
        loadMainDataSet(activeSymbol, activeTimeframe);
        CandleSeries liveSeries = mainDataSet == null ? null : mainDataSet.getSeries();
        if (previousSeries != null && previousViewport != null && liveSeries != null && !previousSeries.isEmpty() && !liveSeries.isEmpty()) {
            int deltaIndex = Math.max(0, liveSeries.size() - previousSeries.size());
            double translateBars = deltaIndex;
            viewport.setX(previousViewport.getXMin() + translateBars, previousViewport.getXMax() + translateBars, xAxisRangePolicy);
            clampX();
        }
        repaint();
    }

    private List<Indicator> indicatorsForLayout(PaneLayout layout) {
        List<Indicator> out = new ArrayList<>();
        if (layout.main()) {
            out.addAll(indicators.all());
            return out;
        }
        if (layout.pane() != null) {
            out.addAll(layout.pane().getIndicators());
        }
        return out;
    }

    private JPopupMenu buildPanePopup(PaneLayout layout) {
        JPopupMenu popup = new JPopupMenu();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inputs", buildPaneInputsTab(layout));
        tabs.addTab("Style", buildPaneStyleTab(layout, popup));
        tabs.addTab("Visibility", buildPaneVisibilityTab(layout));
        tabs.addTab("Indicators", buildPaneIndicatorsTab(layout));
        popup.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        popup.add(tabs);
        return popup;
    }

    private JPanel buildPaneInputsTab(PaneLayout layout) {
        JPanel panel = verticalTabPanel();
        panel.add(tabInfoLabel("Title: " + layout.title()));
        panel.add(tabInfoLabel("Location: " + layout.pane().getLocation()));
        panel.add(tabInfoLabel(String.format(Locale.US, "Height ratio: %.2f", layout.pane().getHeightRatio())));
        JCheckBox autoFitBox = new JCheckBox("Auto fit Y", layout.pane().isAutoFitY());
        autoFitBox.setOpaque(false);
        autoFitBox.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        autoFitBox.addActionListener(e -> {
            layout.pane().setAutoFitY(autoFitBox.isSelected());
            if (autoFitBox.isSelected()) {
                fitPaneY(layout.pane());
            }
            repaint();
        });
        panel.add(autoFitBox);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildPaneStyleTab(PaneLayout layout, JPopupMenu popup) {
        JPanel panel = verticalTabPanel();
        panel.add(tabInfoLabel("Shared Y axis: " + axisYLocationFor(layout)));
        panel.add(tabInfoLabel("Background: Gradient"));
        if (layout.pane().getLocation() == PaneLocation.STACKED) {
            JButton collapseButton = new JButton(stateFor(layout.pane()).collapsed() ? "Expand" : "Collapse");
            collapseButton.addActionListener(e -> {
                setPaneCollapsed(layout.pane(), !stateFor(layout.pane()).collapsed());
                popup.setVisible(false);
            });
            panel.add(collapseButton);
        }
        JButton destroyButton = new JButton("Destroy Window");
        destroyButton.addActionListener(e -> {
            destroyPane(layout.pane());
            popup.setVisible(false);
        });
        panel.add(destroyButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildPaneVisibilityTab(PaneLayout layout) {
        JPanel panel = verticalTabPanel();
        JCheckBox visibleBox = new JCheckBox("Visible", layout.pane().isVisible());
        visibleBox.setOpaque(false);
        visibleBox.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        visibleBox.addActionListener(e -> {
            layout.pane().setVisible(visibleBox.isSelected());
            repaint();
        });
        panel.add(visibleBox);
        panel.add(tabInfoLabel("Timeframes"));
        for (Timeframe timeframe : Timeframe.values()) {
            JCheckBox box = new JCheckBox(timeframe.name(), layout.pane().isVisibleOnTimeframe(timeframe));
            box.setOpaque(false);
            box.setForeground(new Color(ChartStyle.COLOR_TEXT_MUTED, true));
            box.addActionListener(e -> {
                layout.pane().setVisibleOnTimeframe(timeframe, box.isSelected());
                repaint();
            });
            panel.add(box);
        }
        return panel;
    }

    private JPanel buildPaneIndicatorsTab(PaneLayout layout) {
        JPanel panel = verticalTabPanel();
        List<Indicator> activeIndicators = indicatorsForLayout(layout);
        if (activeIndicators.isEmpty()) {
            panel.add(tabInfoLabel("No indicators on this pane"));
            panel.add(Box.createVerticalGlue());
            return panel;
        }
        for (Indicator indicator : activeIndicators) {
            panel.add(tabInfoLabel(indicator.getName()));
            if (indicator instanceof ConfigurableIndicator configurable) {
                for (IndicatorProperty property : configurable.getProperties()) {
                    JPanel row = new JPanel(new BorderLayout(8, 0));
                    row.setOpaque(false);
                    JLabel name = new JLabel(property.label());
                    name.setForeground(new Color(ChartStyle.COLOR_TEXT_MUTED, true));
                    row.add(name, BorderLayout.WEST);
                    JTextField field = new JTextField(property.value(), 10);
                    field.setEnabled(property.editable());
                    field.addActionListener(e -> {
                        if (property.editable() && configurable.applyProperty(property.key(), field.getText())) {
                            ensureIndicatorsCalculated();
                            repaint();
                        }
                    });
                    field.addFocusListener(new java.awt.event.FocusAdapter() {
                        @Override
                        public void focusLost(java.awt.event.FocusEvent e) {
                            if (property.editable() && configurable.applyProperty(property.key(), field.getText())) {
                                ensureIndicatorsCalculated();
                                repaint();
                            } else {
                                field.setText(property.value());
                            }
                        }
                    });
                    row.add(field, BorderLayout.CENTER);
                    panel.add(row);
                }
            } else {
                panel.add(tabInfoLabel("No editable properties"));
            }
            panel.add(Box.createVerticalStrut(8));
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel verticalTabPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(ChartStyle.COLOR_BG_TOP, true));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return panel;
    }

    private JLabel tabInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(ChartStyle.COLOR_TEXT, true));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void paintWindowChrome(Graphics2D graphics) {
        for (PaneLayout layout : paneLayouts) {
            if (layout.overlay() || layout.main()) {
                continue;
            }
            Rectangle header = layout.headerBounds();
            if (header.height <= 0) {
                continue;
            }
            PaneWindowState state = stateFor(layout.pane());
            boolean mouseInsideWindow = layout.bounds().contains(mouseX, mouseY);
            int revealAlpha = mouseInsideWindow ? HEADER_REVEAL_ALPHA_MAX : 0;

            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, revealAlpha / 255f))));
            graphics.setColor(new Color(245, 248, 255, 255));
            graphics.setFont(getFont().deriveFont(Font.BOLD, 14f));
            graphics.drawString(layout.title(), header.x + PANE_HEADER_PADDING_PX, header.y + 22);
            graphics.setColor(new Color(180, 180, 180, 220));
            graphics.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            graphics.drawString(state.collapsed() ? "Collapsed" : "Expanded", header.x + PANE_HEADER_PADDING_PX + 120, header.y + 22);

            for (HeaderButton button : headerButtonsFor(layout)) {
                boolean hot = hoveredHeaderAction != null && hoveredHeaderAction.layout() == layout && hoveredHeaderAction.action() == button.action();
                int fillAlpha = hot ? 164 : 72;
                int strokeAlpha = hot ? 220 : 110;
                int textAlpha = hot ? 255 : 228;
                graphics.setColor(new Color(30, 36, 48, fillAlpha));
                graphics.fillRoundRect(button.bounds().x, button.bounds().y, button.bounds().width, button.bounds().height, 10, 10);
                graphics.setColor(new Color(180, 196, 224, strokeAlpha));
                graphics.drawRoundRect(button.bounds().x, button.bounds().y, button.bounds().width, button.bounds().height, 10, 10);
                graphics.setColor(new Color(255, 255, 255, textAlpha));
                graphics.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                graphics.drawString(button.label(), button.bounds().x + 8, button.bounds().y + 15);
            }
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    private List<HeaderButton> headerButtonsFor(PaneLayout layout) {
        if (layout.overlay() || layout.main()) {
            return List.of();
        }
        List<String> actions = new ArrayList<>();
        actions.add("INSERT_ABOVE");
        actions.add("INSERT_BELOW");
        actions.add("MOVE_TOP");
        actions.add("MOVE_BOTTOM");
        actions.add(stateFor(layout.pane()).collapsed() ? "EXPAND" : "COLLAPSE");
        actions.add("DESTROY");
        int x = layout.headerBounds().x + layout.headerBounds().width - PANE_HEADER_PADDING_PX;
        List<HeaderButton> buttons = new ArrayList<>();
        for (int i = actions.size() - 1; i >= 0; i--) {
            String action = actions.get(i);
            String label = headerActionLabel(action);
            int width = buttonWidthFor(label);
            x -= width;
            Rectangle bounds = new Rectangle(x, layout.headerBounds().y + (layout.headerBounds().height - HEADER_BUTTON_HEIGHT_PX) / 2, width, HEADER_BUTTON_HEIGHT_PX);
            buttons.add(0, new HeaderButton(action, label, bounds));
            x -= HEADER_BUTTON_GAP_PX;
        }
        return buttons;
    }

    private int buttonWidthFor(String label) {
        return Math.max(68, label.length() * 7 + 16);
    }

    private String headerActionLabel(String action) {
        return switch (action) {
            case "ADD_WINDOW" -> "Add Window";
            case "INSERT_ABOVE" -> "Insert Above";
            case "INSERT_BELOW" -> "Insert Below";
            case "MOVE_TOP" -> "Move To Top";
            case "MOVE_BOTTOM" -> "Move To Bottom";
            case "COLLAPSE" -> "Collapse";
            case "EXPAND" -> "Expand";
            case "DESTROY" -> "Destroy";
            default -> action;
        };
    }

    private HeaderActionHit findHeaderActionAt(int x, int y) {
        for (int i = paneLayouts.size() - 1; i >= 0; i--) {
            PaneLayout layout = paneLayouts.get(i);
            if (layout.overlay() || layout.main() || !layout.headerBounds().contains(x, y)) {
                continue;
            }
            for (HeaderButton button : headerButtonsFor(layout)) {
                if (button.bounds().contains(x, y)) {
                    return new HeaderActionHit(layout, button.action());
                }
            }
        }
        return null;
    }

    private void performHeaderAction(HeaderActionHit hit) {
        PaneLayout layout = hit.layout();
        switch (hit.action()) {
            case "INSERT_ABOVE" -> insertRuntimePaneRelative(layout.pane(), true);
            case "INSERT_BELOW" -> insertRuntimePaneRelative(layout.pane(), false);
            case "MOVE_TOP" -> movePane(layout.pane(), true);
            case "MOVE_BOTTOM" -> movePane(layout.pane(), false);
            case "COLLAPSE" -> setPaneCollapsed(layout.pane(), true);
            case "EXPAND" -> setPaneCollapsed(layout.pane(), false);
            case "DESTROY" -> destroyPane(layout.pane());
            default -> {
            }
        }
    }

    private void setPaneCollapsed(ChartPane pane, boolean collapsed) {
        if (pane == null) {
            return;
        }
        stateFor(pane).setCollapsed(collapsed);
        PaneLayout layout = null;
        for (PaneLayout candidate : paneLayouts) {
            if (candidate.pane() == pane) {
                layout = candidate;
                break;
            }
        }
        ParamsBox box = computeSharedBox();
        int visibleSegmentCount = (int) paneLayouts.stream().filter(l -> !l.overlay()).count();
        int totalSegmentGap = Math.max(0, visibleSegmentCount - 1) * STACKED_GAP_PX;
        int totalSegmentHeight = Math.max(1, box.contentHeight() - totalSegmentGap);
        if (collapsed) {
            pane.setHeightRatio(Math.max(0.0001, PANE_HEADER_HEIGHT_PX / (double) totalSegmentHeight));
        } else if (layout != null && layout.bounds().height <= PANE_HEADER_HEIGHT_PX) {
            pane.setHeightRatio(Math.max(0.12, 140.0 / totalSegmentHeight));
        }
        syncMainPaneRatio(new ArrayList<>(panes.stream().filter(p -> p.getLocation() == PaneLocation.STACKED).toList()));
        rebuildPaneLayouts();
        repaint();
    }

    private void destroyPane(ChartPane pane) {
        if (pane == null) {
            return;
        }
        panes.remove(pane);
        paneWindowStates.remove(pane.getId());
        paneLayouts.removeIf(layout -> layout.pane() == pane);
        if (activeInteractionPane != null && activeInteractionPane.pane() == pane) {
            activeInteractionPane = null;
        }
        repaint();
    }

    private void insertRuntimePaneRelative(ChartPane anchor, boolean above) {
        if (anchor == null) {
            addRuntimeStackedPane();
            return;
        }
        int anchorIndex = panes.indexOf(anchor);
        if (anchorIndex < 0) {
            addRuntimeStackedPane();
            return;
        }
        ChartPane pane = new ChartPane("runtime-" + runtimePaneCounter, "Window " + runtimePaneCounter)
                .setLocation(PaneLocation.STACKED)
                .setHeightRatio(Math.max(0.12, Math.min(0.24, anchor.getHeightRatio() * 0.8)))
                .setYAxisRangePolicy(new AxisRangePolicy().setNonNegative(false))
                .addDataSet(buildRuntimePaneDataSet(runtimePaneCounter));
        runtimePaneCounter++;
        panes.add(above ? anchorIndex : anchorIndex + 1, pane);
        stateFor(pane);
        repaint();
    }

    private void movePane(ChartPane pane, boolean toTop) {
        if (pane == null) {
            return;
        }
        List<ChartPane> stacked = new ArrayList<>();
        List<ChartPane> others = new ArrayList<>();
        for (ChartPane candidate : panes) {
            if (candidate.getLocation() == PaneLocation.STACKED) {
                stacked.add(candidate);
            } else {
                others.add(candidate);
            }
        }
        int currentIndex = stacked.indexOf(pane);
        if (currentIndex < 0) {
            return;
        }
        stacked.remove(currentIndex);
        if (toTop) {
            stacked.add(0, pane);
            mainOrderIndex = Math.min(mainOrderIndex + 1, stacked.size());
        } else {
            stacked.add(pane);
            mainOrderIndex = Math.min(mainOrderIndex, stacked.size());
        }
        panes.clear();
        panes.addAll(others);
        panes.addAll(stacked);
        repaint();
    }

    private int getMainPaneMinHeightPx() {
        return PANE_HEADER_HEIGHT_PX;
    }

    private int getStackedPaneMinHeightPx() {
        return PANE_HEADER_HEIGHT_PX;
    }

}

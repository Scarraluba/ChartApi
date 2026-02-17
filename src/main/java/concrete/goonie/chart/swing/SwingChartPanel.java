package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.data.DataSetCollection;
import concrete.goonie.chart.api.data.ValueDataSet;
import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.indicator.Indicator;
import concrete.goonie.chart.api.indicator.IndicatorSet;
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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
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
import java.util.Locale;
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
    private double bidPrice = Double.NaN;
    private double askPrice = Double.NaN;
    private double pipSize = 0.01;
    private final Timer replayTimer;

private static final int STACKED_GAP_PX = 6;
private static final int SPLITTER_HIT_HALF_HEIGHT_PX = 5;
private int hoveredSplitterIndex = -1;
private int activeSplitterIndex = -1;
private boolean resizingStackedPanes;
private boolean replaySelectionPannedThisDrag;
private final List<ChartSplitPane> stackedSplitPanes = new ArrayList<>();
private boolean suppressSplitPaneSync;

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
        setLayout(null);
        setFocusable(true);
        setBackground(new Color(ChartStyle.COLOR_BG, true));

        CandleSeries series = CandleSeries.generateDemo(LocalDateTime.of(2025, 1, 1, 0, 0), Timeframe.M1, 4000);
        historicalData.setSeries(activeSymbol, Timeframe.M1, series);
        loadMainDataSet(activeSymbol, activeTimeframe);
        replayTimer = new Timer(500, event -> replayForward());
        installInputHandlers();
    }

    public HistoricalData getHistoricalData() { return historicalData; }
    public IndicatorSet getIndicators() { return indicators; }
    public ToolSet getTools() { return tools; }
    public DataSetCollection getCustomDataSets() { return customDataSets; }
    public List<ChartPane> getPanes() { return Collections.unmodifiableList(panes); }
    public Timeframe getActiveTimeframe() { return activeTimeframe; }
    public RenderOptimizationOptions getOptimizationOptions() { return optimizationOptions; }

    public void addPane(ChartPane pane) {
        if (pane != null && !panes.contains(pane)) {
            panes.add(pane);
            repaint();
        }
    }

    public void clearPanes() {
        panes.clear();
        clearStackedSplitPaneOverlay();
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
            remapToolsToSeries(previousSeries, series);
        }
        updateBidAskFromLastCandle(series);
        viewport = createViewportForSeries(series, previousSeries, previousViewport, previousTimeframe, timeframe);
        viewport.applyXPolicy(xAxisRangePolicy);
        clampX();
        viewport.applyYPolicy(yAxisRangePolicy);
        indicators.calculateAll(historicalData, symbol);
        fitYNow();
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return tooltipInfo == null ? null : tooltipInfo.text();
    }

    @Override
    protected void paintComponent(Graphics graphics0) {
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
            renderList.clear();

            for (PaneLayout layout : paneLayouts) {
                paintPane(layout);
            }

            paintStackedSplitters(renderList);

            if (tooltipInfo != null) {
                paintTooltip(renderList, tooltipInfo);
            }
            swingChartRenderer.draw(graphics, renderList);
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

        int stackedGapTotal = Math.max(0, stacked.size() - 1) * STACKED_GAP_PX;
        int requestedStackedTotal = 0;
        for (ChartPane pane : stacked) {
            requestedStackedTotal += Math.round(box.contentHeight() * pane.getHeightRatio());
        }
        int stackedTotal = Math.min(requestedStackedTotal, Math.max(0, box.contentHeight() - 120 - stackedGapTotal));
        int mainHeight = Math.max(120, box.contentHeight() - stackedTotal - stackedGapTotal);
        Rectangle mainBounds = new Rectangle(box.left(), box.top(), box.width(), mainHeight);
        paneLayouts.add(new PaneLayout(null, "Main", mainBounds, viewport, true, false));

        int y = box.top() + mainHeight + (stacked.isEmpty() ? 0 : STACKED_GAP_PX);
        List<Integer> stackedHeights = StackedPaneSplitLayout.computeHeights(stacked, stackedTotal, STACKED_GAP_PX);
        int stackedTop = y;
        for (int i = 0; i < stacked.size(); i++) {
            ChartPane pane = stacked.get(i);
            int paneHeight = stackedHeights.get(i);
            Rectangle bounds = new Rectangle(box.left(), y, box.width(), paneHeight);
            paneLayouts.add(new PaneLayout(pane, pane.getTitle(), bounds, pane.getViewport(), false, false));
            y += paneHeight + (i == stacked.size() - 1 ? 0 : STACKED_GAP_PX);
        }
        syncStackedSplitPaneOverlay(stacked, stackedHeights, box.left(), stackedTop, box.width(), STACKED_GAP_PX);

        Rectangle mainRect = mainBounds;
        int overlayIndex = 0;
        for (ChartPane pane : overlays) {
            int overlayHeight = Math.max(84, (int) Math.round(mainRect.height * pane.getHeightRatio()));
            int overlayLeft = mainRect.x;
            int overlayTop = mainRect.y + mainRect.height - overlayHeight - 8 - overlayIndex * 10;
            Rectangle bounds = new Rectangle(overlayLeft, overlayTop, mainRect.width, overlayHeight);
            paneLayouts.add(new PaneLayout(pane, pane.getTitle(), bounds, pane.getViewport(), false, true));
            overlayIndex++;
        }
    }

    private ParamsBox computeSharedBox() {
        int reservedY = axisYLocation == AxisYLocation.NONE ? 0 : params.getYAxisLabelWidthPx() + params.getAxisLabelGapPx();
        int reservedTop = axisXLocation == AxisXLocation.TOP ? params.getXAxisLabelHeightPx() + params.getAxisLabelGapPx() : 0;
        int reservedBottom = axisXLocation == AxisXLocation.BOTTOM ? params.getXAxisLabelHeightPx() + params.getAxisLabelGapPx() : 0;
        int left = params.getContentPadLeft() + (axisYLocation == AxisYLocation.LEFT ? reservedY : 0);
        int right = getWidth() - params.getContentPadRight() - (axisYLocation == AxisYLocation.RIGHT ? reservedY : 0);
        int top = params.getContentPadTop() + reservedTop;
        int bottom = getHeight() - params.getContentPadBottom() - reservedBottom;
        return new ParamsBox(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private void paintPane(PaneLayout layout) {
        AxisXLocation paneXAxis = axisXLocationFor(layout);
        AxisYLocation paneYAxis = axisYLocation;
        ChartEngine.Params paneParams = new ChartEngine.Params()
                .setViewWidthPx(getWidth())
                .setViewHeightPx(getHeight())
                .setDrawAxisLabels(true)
                .setAxisXLocation(paneXAxis)
                .setAxisYLocation(paneYAxis)
                .setXAxisLabelFormatter(this::formatXAxisWorldValue)
                .setXAxisLabelProvider(this::buildXAxisLabel)
                .setDrawCrosshair(isCrosshairPane(layout))
                .setCrosshairScreenX(mouseX)
                .setCrosshairScreenY(mouseY);

        ChartEngine.ChartRect rect = new ChartEngine.ChartRect(layout.bounds().x, layout.bounds().y, layout.bounds().width, layout.bounds().height);

        if (layout.overlay()) {
            renderList.add(new RenderCommand.RoundedRect(rect.left() - 1f, rect.top() - 1f, rect.right() + 1f, rect.bottom() + 1f, 12f, new Style(0xCC0F172A, 1f, true).withCornerRadius(12f)));
            renderList.add(new RenderCommand.RoundedRect(rect.left() - 1f, rect.top() - 1f, rect.right() + 1f, rect.bottom() + 1f, 12f, new Style(ChartStyle.COLOR_PANEL_BORDER, 1f, false).withCornerRadius(12f)));
            renderList.add(new RenderCommand.Text(rect.left() + 10f, rect.top() + 16f, layout.title(), 11f, new Style(ChartStyle.COLOR_TEXT_MUTED, 1f, false)));
        }

        chartEngine.buildBaseIntoRect(renderList, layout.viewport(), paneParams, rect, false);
        Transform transform = new Transform(rect.left(), rect.top(), rect.width(), rect.height(), layout.viewport());
        int from = (int) Math.floor(viewport.getXMin());
        int to = (int) Math.ceil(viewport.getXMax());

        if (layout.main()) {
            mainDataSet.render(renderList, transform, from, to);
            for (DataSet dataSet : mainPaneDataSets()) {
                if (dataSet.isVisible() && dataSet.supports(activeTimeframe)) {
                    if (dataSet instanceof concrete.goonie.chart.api.data.LineDataSet lineDataSet) lineDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof concrete.goonie.chart.api.data.BarDataSet barDataSet) barDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof CandlestickDataSet candlestickDataSet) candlestickDataSet.setOptimizationOptions(optimizationOptions);
                    if (dataSet instanceof concrete.goonie.chart.api.data.LineDataSet lineDataSet) lineDataSet.setOptimizationOptions(optimizationOptions);
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
            paintReplayOverlay(transform, rect);
        } else if (layout.pane() != null) {
            for (DataSet dataSet : paneDataSets(layout.pane())) {
                if (dataSet.isVisible() && dataSet.supports(activeTimeframe)) {
                    dataSet.render(renderList, transform, from, to);
                }
            }
        }
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

    private void paintReplayOverlay(Transform transform, ChartEngine.ChartRect rect) {
        if (!replayState.isOverlayVisible()) {
            return;
        }
        renderList.add(new RenderCommand.RoundedRect(rect.left() + 12f, rect.top() + 12f, rect.left() + 300f, rect.top() + 62f, 10f, new Style(0xCC0B1220, 1f, true).withCornerRadius(10f)));
        renderList.add(new RenderCommand.RoundedRect(rect.left() + 12f, rect.top() + 12f, rect.left() + 300f, rect.top() + 62f, 10f, new Style(0xFF60A5FA, 1f, false).withCornerRadius(10f)));
        String stateText = replayState.isAwaitingStartSelection()
                ? (replayState.isXPanSelectionEnabled() ? "Move cursor or pan X, then click to choose replay start" : "Click a bar to choose replay start")
                : "Replay start: " + formatSourceIndexAsTime(replayState.getSelectedSourceIndex());
        renderList.add(new RenderCommand.Text(rect.left() + 24f, rect.top() + 34f, "Replay / Backtest", 12f, new Style(0xFFF8FAFC, 1f, false)));
        renderList.add(new RenderCommand.Text(rect.left() + 24f, rect.top() + 52f, stateText, 11f, new Style(0xFFCBD5E1, 1f, false)));
        int trackedIndex = replayState.getTrackedSourceIndex() >= 0
                ? replayState.getTrackedSourceIndex()
                : (replayState.getSelectedSourceIndex() >= 0 ? replayState.getSelectedSourceIndex() : clampSourceIndexForViewportRightEdge());
        trackedIndex = Math.max(0, Math.min(mainDataSet.getSeries().size() - 1, trackedIndex));
        int trackedX = transform.worldToScreenX(trackedIndex);
        renderList.add(new RenderCommand.RoundedRect(rect.left(), rect.top(), rect.right(), rect.bottom(), 0f, new Style(0x52101727, 1f, true)));
        renderList.add(new RenderCommand.Line(trackedX, rect.top(), trackedX, rect.bottom(), new Style(0xFF38BDF8, 4f, false)));
    }

private void paintStackedSplitters(RenderList out) {
    updateSplitPaneVisualState();
}






    private AxisXLocation axisXLocationFor(PaneLayout layout) {
        long stackedCount = paneLayouts.stream().filter(p -> !p.main() && !p.overlay()).count();
        if (layout.overlay()) {
            return AxisXLocation.NONE;
        }
        if (stackedCount == 0) {
            return axisXLocation;
        }
        PaneLayout lastStacked = null;
        for (PaneLayout paneLayout : paneLayouts) {
            if (!paneLayout.main() && !paneLayout.overlay()) {
                lastStacked = paneLayout;
            }
        }
        return layout == lastStacked ? axisXLocation : AxisXLocation.NONE;
    }

    private boolean isCrosshairPane(PaneLayout layout) {
        return mouseX >= 0 && mouseY >= 0 && layout.bounds().contains(mouseX, mouseY);
    }

    private void updateSplitPaneVisualState() {
        // Visual hover/active state is handled by ChartSplitPane dividers directly.
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

                if (!resizingStackedPanes) {
                    Transform transform = transformFor(interactionPane);
                    if (activeDragTool != null && activeControlPointIndex >= 0 && interactionPane.main()) {
                        activeDragTool.moveControlPoint(activeControlPointIndex, transform.screenToWorldX(event.getX()), transform.screenToWorldY(event.getY()));
                    } else if (activeDragTool != null && interactionPane.main()) {
                        double dxWorld = transform.screenToWorldX(event.getX()) - transform.screenToWorldX(lastX);
                        double dyWorld = transform.screenToWorldY(event.getY()) - transform.screenToWorldY(lastY);
                        activeDragTool.translate(dxWorld, dyWorld);
                    } else {
                        panByPixels(interactionPane, event.getX() - lastX, event.getY() - lastY);
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
                activeSplitterIndex = findHoveredSplitterIndex(event.getX(), event.getY());
                resizingStackedPanes = activeSplitterIndex >= 0;
                activeInteractionPane = findPaneLayoutAt(event.getX(), event.getY());

                if (activeInteractionPane == null && !resizingStackedPanes) {
                    clearSelection();
                    dragging = false;
                    repaint();
                    return;
                }


                if (activeInteractionPane.main()) {
                    if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && !replayState.isXPanSelectionEnabled()) {
                        Transform transform = transformFor(activeInteractionPane);
                        int selectedIndex = clampSourceIndexForScreenX(event.getX(), transform);
                        selectReplayStartIndex(selectedIndex);
                        dragging = false;
                        updateHoverState(event);
                        repaint();
                        return;
                    }
                    ToolHit toolHit = findToolHit(event.getX(), event.getY(), activeInteractionPane);
                    if (toolHit != null) {
                        clearSelection();
                        activeDragTool = toolHit.tool();
                        activeControlPointIndex = toolHit.controlPointIndex();
                        activeDragTool.setSelected(true);
                    } else {
                        clearSelection();
                    }
                }
                dragging = true;
                updateHoverState(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && replayState.isXPanSelectionEnabled()
                        && activeInteractionPane != null && activeInteractionPane.main() && !resizingStackedPanes
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
                hoveredSplitterIndex = -1;
                activeSplitterIndex = -1;
                resizingStackedPanes = false;
                updateToolStates(null, null);
                setCursor(Cursor.getDefaultCursor());
                updateSplitPaneVisualState();
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
            if (hovered.main()) {
                zoomAt(hovered, event.getX(), event.getY(), factor, true, event.isShiftDown());
            } else {
                zoomAt(hovered, event.getX(), event.getY(), factor, true, false);
            }
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
                }
            }
        });
    }

    private void panByPixels(PaneLayout pane, int dxPixels, int dyPixels) {
        Transform transform = transformFor(pane);
        double dxWorld = -dxPixels / transform.pixelsPerWorldX();
        viewport.setX(viewport.getXMin() + dxWorld, viewport.getXMax() + dxWorld);
        clampX();
        if (pane.main()) {
            double dyWorld = dyPixels / transform.pixelsPerWorldY();
            viewport.setY(viewport.getYMin() + dyWorld, viewport.getYMax() + dyWorld);
        }
        if (autoFitY) {
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
        return clampMainSeriesIndexForWorldX(viewport.getXMax());
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
        hoveredSplitterIndex = findHoveredSplitterIndex(mouseX, mouseY);
        PaneLayout hoveredPane = findPaneLayoutAt(mouseX, mouseY);
        updateSplitPaneVisualState();
        if (replayState.isOverlayVisible() && replayState.isAwaitingStartSelection() && hoveredPane != null && hoveredPane.main()) {
            Transform transform = transformFor(hoveredPane);
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
        for (Tool tool : tools.all()) {
            tool.setSelected(false);
        }
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
        for (int i = renderable.size() - 1; i >= 0; i--) {
            Tool tool = renderable.get(i);
            if (!tool.isVisible()) {
                continue;
            }
            int controlPointIndex = tool.hitTestControlPoint(transform, x, y, 9.0);
            if (controlPointIndex >= 0) {
                return new ToolHit(tool, controlPointIndex);
            }
            if (tool.hitTest(transform, x, y)) {
                return new ToolHit(tool, -1);
            }
        }
        return null;
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
        Rectangle bounds = layout.bounds();
        return new Transform(bounds.x, bounds.y, bounds.width, bounds.height, layout.viewport());
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
        currentBaseSeries = realTimeBaseSeries;
        loadMainDataSet(activeSymbol, activeTimeframe);
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
        if (mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
            return String.format(Locale.US, "%.0f", xWorld);
        }
        int index = clampMainSeriesIndexForWorldX(xWorld);
        LocalDateTime current = mainDataSet.getSeries().get(index).time();
        LocalDateTime previous = findPreviousAxisLabelTime(xWorld, index);
        return getLabel(current, previous);
    }

    private ChartEngine.AxisLabel buildXAxisLabel(double xWorld) {
        if (mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
            return new ChartEngine.AxisLabel(String.format(Locale.US, "%.0f", xWorld), 12f, Font.PLAIN);
        }
        int index = clampMainSeriesIndexForWorldX(xWorld);
        LocalDateTime current = mainDataSet.getSeries().get(index).time();
        LocalDateTime previous = findPreviousAxisLabelTime(xWorld, index);
        Font font = getLabelFont(current, previous);
        return new ChartEngine.AxisLabel(getLabel(current, previous), font.getSize2D(), font.getStyle());
    }

    private LocalDateTime findPreviousAxisLabelTime(double xWorld, int currentIndex) {
        if (viewport == null || mainDataSet == null || mainDataSet.getSeries().isEmpty()) {
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
        int previousIndex = clampMainSeriesIndexForWorldX(previousWorldX);
        if (previousIndex == currentIndex && currentIndex > 0) {
            previousIndex = currentIndex - 1;
        }
        return mainDataSet.getSeries().get(previousIndex).time();
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

    private String formatSourceIndexAsTime(int sourceIndex) {
        if (realTimeBaseSeries == null || realTimeBaseSeries.isEmpty() || sourceIndex < 0) {
            return "-";
        }
        int clamped = Math.max(0, Math.min(realTimeBaseSeries.size() - 1, sourceIndex));
        return realTimeBaseSeries.get(clamped).time().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
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
        int mainIndex = clampMainSeriesIndexForWorldX(transform.screenToWorldX(screenX));
        LocalDateTime time = mainDataSet.getSeries().get(mainIndex).time();
        int index = realTimeBaseSeries.indexAtOrBefore(time);
        if (index < 0) {
            return 0;
        }
        return Math.max(0, Math.min(realTimeBaseSeries.size() - 1, index));
    }

    private void remapToolsToSeries(CandleSeries oldSeries, CandleSeries newSeries) {
        if (oldSeries == null || newSeries == null || oldSeries.isEmpty() || newSeries.isEmpty()) {
            return;
        }
        for (Tool tool : tools.all()) {
            List<Tool.ControlPoint> points = tool.controlPoints();
            for (int i = 0; i < points.size(); i++) {
                Tool.ControlPoint point = points.get(i);
                int oldIndex = Math.max(0, Math.min(oldSeries.size() - 1, (int) Math.round(point.xWorld())));
                LocalDateTime time = oldSeries.get(oldIndex).time();
                int newIndex = newSeries.indexAtOrBefore(time);
                if (newIndex < 0) {
                    newIndex = 0;
                }
                tool.moveControlPoint(i, newIndex, point.yWorld());
            }
        }
    }

    private void syncStackedSplitPaneOverlay(List<ChartPane> stacked, List<Integer> stackedHeights, int left, int top, int width, int gapPx) {
        if (stacked.isEmpty()) {
            clearStackedSplitPaneOverlay();
            return;
        }

        int overlayHeight = 0;
        for (Integer stackedHeight : stackedHeights) {
            overlayHeight += stackedHeight;
        }
        overlayHeight += Math.max(0, stacked.size() - 1) * gapPx;

        suppressSplitPaneSync = true;
        try {
            while (stackedSplitPanes.size() < Math.max(0, stacked.size() - 1)) {
                ChartSplitPane splitPane = new ChartSplitPane(gapPx);
                splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
                    if (!suppressSplitPaneSync) {
                        applyStackedOverlayHeights();
                    }
                });
                stackedSplitPanes.add(splitPane);
                add(splitPane);
            }
            for (int i = 0; i < stackedSplitPanes.size(); i++) {
                stackedSplitPanes.get(i).setVisible(i < stacked.size() - 1);
            }
            if (!stackedSplitPanes.isEmpty()) {
                ChartSplitPane root = stackedSplitPanes.get(0);
                root.setBounds(left, top, width, overlayHeight);
                configureSplitPaneChain(0, stackedHeights, width, overlayHeight, gapPx);
                root.revalidate();
            }
        } finally {
            suppressSplitPaneSync = false;
        }
        repaint();
    }

    private JComponent configureSplitPaneChain(int splitIndex, List<Integer> stackedHeights, int width, int availableHeight, int gapPx) {
        if (splitIndex >= stackedHeights.size() - 1) {
            JPanel leaf = new JPanel();
            leaf.setOpaque(false);
            leaf.setPreferredSize(new java.awt.Dimension(width, Math.max(1, availableHeight)));
            return leaf;
        }

        ChartSplitPane splitPane = stackedSplitPanes.get(splitIndex);
        splitPane.setVisible(true);
        splitPane.setBounds(0, 0, width, Math.max(1, availableHeight));
        splitPane.getTopProxy().setPreferredSize(new java.awt.Dimension(width, stackedHeights.get(splitIndex)));

        int bottomHeight = Math.max(1, availableHeight - stackedHeights.get(splitIndex) - gapPx);
        JComponent bottomComponent = configureSplitPaneChain(splitIndex + 1, stackedHeights, width, bottomHeight, gapPx);
        if (splitPane.getTopComponent() != splitPane.getTopProxy()) {
            splitPane.setTopComponent(splitPane.getTopProxy());
        }
        if (splitPane.getBottomComponent() != bottomComponent) {
            splitPane.setBottomComponent(bottomComponent);
        }
        splitPane.setDividerLocation(stackedHeights.get(splitIndex));
        splitPane.revalidate();
        return splitPane;
    }

    private void clearStackedSplitPaneOverlay() {
        for (ChartSplitPane splitPane : stackedSplitPanes) {
            splitPane.setVisible(false);
        }
    }

    private void applyStackedOverlayHeights() {
        if (stackedSplitPanes.isEmpty() || !stackedSplitPanes.get(0).isVisible()) {
            return;
        }

        List<ChartPane> stackedPanes = new ArrayList<>();
        for (PaneLayout layout : paneLayouts) {
            if (!layout.main() && !layout.overlay() && layout.pane() != null) {
                stackedPanes.add(layout.pane());
            }
        }
        if (stackedPanes.isEmpty()) {
            return;
        }

        List<Integer> heights = new ArrayList<>();
        collectOverlayLeafHeights(stackedSplitPanes.get(0), heights);
        if (heights.size() != stackedPanes.size()) {
            return;
        }

        int total = 0;
        for (Integer height : heights) {
            total += height;
        }
        if (total <= 0) {
            return;
        }

        for (int i = 0; i < stackedPanes.size(); i++) {
            stackedPanes.get(i).setHeightRatio(heights.get(i) / (double) total);
        }

        suppressSplitPaneSync = true;
        try {
            rebuildPaneLayouts();
            syncPaneViewports();
        } finally {
            suppressSplitPaneSync = false;
        }
        repaint();
    }

    private void collectOverlayLeafHeights(java.awt.Component component, List<Integer> heights) {
        if (component instanceof ChartSplitPane splitPane) {
            collectOverlayLeafHeights(splitPane.getTopComponent(), heights);
            collectOverlayLeafHeights(splitPane.getBottomComponent(), heights);
            return;
        }
        if (component != null) {
            heights.add(component.getHeight());
        }
    }

    private int findHoveredSplitterIndex(int x, int y) {
        for (int i = 0; i < stackedSplitPanes.size(); i++) {
            ChartSplitPane splitPane = stackedSplitPanes.get(i);
            if (!splitPane.isVisible()) {
                continue;
            }
            java.awt.Component divider = ((javax.swing.plaf.basic.BasicSplitPaneUI) splitPane.getUI()).getDivider();
            if (divider == null || divider.getParent() == null) {
                continue;
            }
            Rectangle bounds = SwingUtilities.convertRectangle(divider.getParent(), divider.getBounds(), this);
            if (bounds.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private record TooltipInfo(int x, int y, String title, String text) {}
    private record ToolHit(Tool tool, int controlPointIndex) {}

    private String buildPaneCacheKey(PaneLayout layout, ChartEngine.ChartRect rect, ChartEngine.Params paneParams) {
        Viewport vp = layout.viewport();
        return layout.title() + '|' + rect.left() + ',' + rect.top() + ',' + rect.width() + ',' + rect.height()
                + '|' + paneParams.getAxisXLocation() + '|' + paneParams.getAxisYLocation()
                + '|' + String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f", vp.getXMin(), vp.getXMax(), vp.getYMin(), vp.getYMax());
    }
    private record Range(double low, double high) {}
    private record ParamsBox(int left, int top, int width, int contentHeight) {}
    private record PaneLayout(ChartPane pane, String title, Rectangle bounds, Viewport viewport, boolean main, boolean overlay) {}
}

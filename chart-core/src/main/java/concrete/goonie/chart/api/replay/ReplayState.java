package concrete.goonie.chart.api.replay;

import concrete.goonie.chart.api.model.Timeframe;

import java.time.LocalDateTime;

/**
 * Mutable replay/backtest state.
 */
public final class ReplayState {
    private boolean overlayVisible;
    private boolean awaitingStartSelection;
    private boolean playing;
    private int selectedSourceIndex = -1;
    private int currentSourceIndex = -1;
    private int speed = 1;
    private Timeframe interval = Timeframe.M1;
    private ReplayStartMode startMode = ReplayStartMode.SELECT_BAR;
    private LocalDateTime selectedStartDate;
    private int trackedSourceIndex = -1;
    private boolean xPanSelectionEnabled;

    public boolean isOverlayVisible() { return overlayVisible; }
    public void setOverlayVisible(boolean overlayVisible) { this.overlayVisible = overlayVisible; }
    public boolean isAwaitingStartSelection() { return awaitingStartSelection; }
    public void setAwaitingStartSelection(boolean awaitingStartSelection) { this.awaitingStartSelection = awaitingStartSelection; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public int getSelectedSourceIndex() { return selectedSourceIndex; }
    public void setSelectedSourceIndex(int selectedSourceIndex) { this.selectedSourceIndex = selectedSourceIndex; }
    public int getCurrentSourceIndex() { return currentSourceIndex; }
    public void setCurrentSourceIndex(int currentSourceIndex) { this.currentSourceIndex = currentSourceIndex; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = Math.max(1, speed); }
    public Timeframe getInterval() { return interval; }
    public void setInterval(Timeframe interval) { this.interval = interval == null ? Timeframe.M1 : interval; }
    public ReplayStartMode getStartMode() { return startMode; }
    public void setStartMode(ReplayStartMode startMode) { this.startMode = startMode == null ? ReplayStartMode.SELECT_BAR : startMode; }
    public LocalDateTime getSelectedStartDate() { return selectedStartDate; }
    public void setSelectedStartDate(LocalDateTime selectedStartDate) { this.selectedStartDate = selectedStartDate; }
    public int getTrackedSourceIndex() { return trackedSourceIndex; }
    public void setTrackedSourceIndex(int trackedSourceIndex) { this.trackedSourceIndex = trackedSourceIndex; }
    public boolean isXPanSelectionEnabled() { return xPanSelectionEnabled; }
    public void setXPanSelectionEnabled(boolean xPanSelectionEnabled) { this.xPanSelectionEnabled = xPanSelectionEnabled; }
}

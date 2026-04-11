# Goonie Chart - Cross Platform Split Target

Recommended target layout:
- chart-core
- chart-swing
- chart-android
- later chart-javafx

This zip restructures the existing Swing-first chart library into a cleaner module-oriented layout.

## Modules

### chart-core
Pure Java chart engine and shared models.
Contains:
- chart models
- data sets
- history loading
- indicators
- tools/drawings
- pane models
- replay state
- viewport/control logic
- render abstractions and internal math helpers

### chart-swing
Desktop Swing host and demo.
Contains:
- SwingChartPanel
- SwingChartRenderer
- ChartHolderView
- stacked pane layout
- demo Main

### chart-android
Reserved adapter module for Android host integration.
This zip includes an adapter plan instead of Android SDK-bound source so the package stays source-clean.

### chart-javafx
Reserved adapter module for JavaFX host integration.
This zip includes an adapter plan for the later port.

## Direction
Build features in chart-core first, then expose them through Swing, Android, and later JavaFX adapters.

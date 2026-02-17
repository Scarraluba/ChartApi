# Goonie Chart API MTFX

Features:
- multi-timeframe historical storage and aggregation
- DataSet API with line, bar, and candlestick dataset types
- timeframe-aware indicators with MT-style `calculateIndicator(...)`
- indicator buffers responsible for rendering
- timeframe-aware tools rendered across selected timeframes
- auto-fit Y support
- Swing demo with control buttons for testing

Main class:
- `concrete.goonie.chart.demo.Main`


Updated demo notes:
- stacked subwindows use a chart-aware custom split layout helper
- hover the top edge of a stacked subwindow to show a resize line and vertical-resize cursor
- drag that split line to resize adjacent stacked panes while keeping chart X alignment

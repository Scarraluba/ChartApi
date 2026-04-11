package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.data.CandlestickDataSet;
import concrete.goonie.chart.api.data.ChartType;
import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.data.render.ChartTypeDataSetFactory;
import concrete.goonie.chart.api.model.Viewport;
import concrete.goonie.chart.api.pane.ChartPane;
import concrete.goonie.chart.api.tool.Tool;
import concrete.goonie.chart.internal.render.RenderList;
import concrete.goonie.chart.internal.render.Transform;

import java.awt.Rectangle;

record TooltipInfo(int x, int y, String title, String text) {}
record ToolHit(Tool tool, int controlPointIndex) {}
record Range(double low, double high) {}
record ParamsBox(int left, int top, int width, int contentHeight) {}
record PaneLayout(ChartPane pane, String title, Rectangle bounds, Rectangle contentBounds, Rectangle headerBounds, Viewport viewport, boolean main, boolean overlay) {}
record HeaderButton(String action, String label, Rectangle bounds) {}
record HeaderActionHit(PaneLayout layout, String action) {}

final class PaneWindowState {
    private boolean collapsed;

    PaneWindowState(boolean collapsed) {
        this.collapsed = collapsed;
    }

    boolean collapsed() {
        return collapsed;
    }

    void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
}

final class SwingChartMainRenderers {
    private SwingChartMainRenderers() {}

    static void renderMainChart(
            ChartType chartType,
            CandlestickDataSet mainDataSet,
            RenderList renderList,
            Transform transform,
            int from,
            int to
    ) {
        DataSet renderable = ChartTypeDataSetFactory.create(mainDataSet, chartType);
        if (renderable != null) {
            renderable.render(renderList, transform, from, to);
        }
    }
}

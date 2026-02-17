
package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.pane.ChartPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Small custom split-layout helper used by the chart so stacked subwindows behave
 * like a chart-aware split pane instead of just fixed painted rectangles.
 *
 * <p>The layout works in chart pixels, preserves total stacked height, keeps a minimum
 * subwindow height, and writes the result back into each pane's height ratio so the
 * geometry stays stable across resizes.</p>
 */
final class StackedPaneSplitLayout {
    private static final int MIN_PANE_HEIGHT_PX = 90;

    private StackedPaneSplitLayout() {
    }

    static List<Integer> computeHeights(List<ChartPane> panes, int totalHeight, int gapPx) {
        List<Integer> heights = new ArrayList<>();
        if (panes.isEmpty()) {
            return heights;
        }
        if (totalHeight <= 0) {
            for (int i = 0; i < panes.size(); i++) {
                heights.add(MIN_PANE_HEIGHT_PX);
            }
            return heights;
        }

        double ratioSum = 0.0;
        for (ChartPane pane : panes) {
            ratioSum += Math.max(0.0001, pane.getHeightRatio());
        }

        int remaining = totalHeight;
        for (int i = 0; i < panes.size(); i++) {
            int height;
            if (i == panes.size() - 1) {
                height = Math.max(MIN_PANE_HEIGHT_PX, remaining);
            } else {
                height = Math.max(MIN_PANE_HEIGHT_PX,
                        (int) Math.round((Math.max(0.0001, panes.get(i).getHeightRatio()) / ratioSum) * totalHeight));
                remaining -= height;
            }
            heights.add(height);
        }

        normalizeHeights(heights, totalHeight);
        return heights;
    }

    static void resize(List<ChartPane> panes, int splitterIndex, int deltaY, int totalHeight, int gapPx) {
        if (panes == null || panes.size() < 2 || splitterIndex < 0 || splitterIndex >= panes.size()) {
            return;
        }

        List<Integer> heights = computeHeights(panes, totalHeight, gapPx);
        int upperIndex = splitterIndex - 1;
        int lowerIndex = splitterIndex;
        if (upperIndex < 0 || lowerIndex >= heights.size()) {
            return;
        }

        int upper = heights.get(upperIndex);
        int lower = heights.get(lowerIndex);

        int newUpper = Math.max(MIN_PANE_HEIGHT_PX, upper + deltaY);
        int newLower = Math.max(MIN_PANE_HEIGHT_PX, lower - (newUpper - upper));

        if (newLower == MIN_PANE_HEIGHT_PX && lower - (newUpper - upper) < MIN_PANE_HEIGHT_PX) {
            newUpper = upper + (lower - MIN_PANE_HEIGHT_PX);
        }
        if (newUpper == MIN_PANE_HEIGHT_PX && upper + deltaY < MIN_PANE_HEIGHT_PX) {
            newLower = lower + (upper - MIN_PANE_HEIGHT_PX);
        }

        heights.set(upperIndex, newUpper);
        heights.set(lowerIndex, newLower);
        normalizeHeights(heights, totalHeight);

        double safeTotal = Math.max(1.0, totalHeight);
        for (int i = 0; i < panes.size(); i++) {
            panes.get(i).setHeightRatio(heights.get(i) / safeTotal);
        }
    }

    private static void normalizeHeights(List<Integer> heights, int totalHeight) {
        int sum = 0;
        for (Integer height : heights) {
            sum += height;
        }
        if (heights.isEmpty() || sum == totalHeight) {
            return;
        }

        int diff = totalHeight - sum;
        int last = heights.size() - 1;
        heights.set(last, Math.max(MIN_PANE_HEIGHT_PX, heights.get(last) + diff));
    }
}

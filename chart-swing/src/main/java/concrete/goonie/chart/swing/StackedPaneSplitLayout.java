package concrete.goonie.chart.swing;

import concrete.goonie.chart.api.pane.ChartPane;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

/**
 * Chart-aware linear stacked layout used by {@link SwingChartPanel}.
 *
 * <p>This mirrors the custom linear layout behavior from the pasted Swing sample:
 * the main pane and stacked panes form one vertical chain, only two adjacent panes
 * are resized by a divider drag, and the resulting pixel sizes are written back into
 * persistent ratios so the layout stays stable across resizes.</p>
 */
final class StackedPaneSplitLayout {
    private static final int MIN_PANE_HEIGHT_PX = 70;
    private static final int MAIN_MIN_HEIGHT_DP = 15;

    private StackedPaneSplitLayout() {
    }

    static List<Integer> computeHeights(double mainWeight, List<ChartPane> panes, int totalHeight, int gapPx) {
        List<Integer> minHeights = new ArrayList<>();
        minHeights.add(getMainMinHeightPx());
        if (panes != null) {
            for (int i = 0; i < panes.size(); i++) {
                minHeights.add(MIN_PANE_HEIGHT_PX);
            }
        }
        return computeHeights(mainWeight, panes, totalHeight, gapPx, minHeights);
    }

    static List<Integer> computeHeights(double mainWeight, List<ChartPane> panes, int totalHeight, int gapPx, List<Integer> minHeights) {
        List<Integer> heights = new ArrayList<>();
        int segmentCount = 1 + (panes == null ? 0 : panes.size());
        if (segmentCount <= 0) {
            return heights;
        }

        int availableHeight = Math.max(0, totalHeight);
        if (segmentCount == 1) {
            int onlyMin = minHeights == null || minHeights.isEmpty() ? getMainMinHeightPx() : Math.max(1, minHeights.get(0));
            heights.add(Math.max(onlyMin, availableHeight));
            return heights;
        }

        List<Double> weights = new ArrayList<>(segmentCount);
        weights.add(Math.max(0.0001, mainWeight));
        if (panes != null) {
            for (ChartPane pane : panes) {
                weights.add(Math.max(0.0001, pane.getHeightRatio()));
            }
        }
        normalizeWeights(weights);

        int used = 0;
        for (int i = 0; i < weights.size(); i++) {
            int min = resolveMinHeight(minHeights, i);
            int size;
            if (i == weights.size() - 1) {
                size = availableHeight - used;
            } else {
                size = (int) Math.round(weights.get(i) * availableHeight);
            }
            size = Math.max(min, size);
            heights.add(size);
            used += size;
        }

        normalizeHeights(heights, availableHeight, minHeights);
        return heights;
    }

    static LayoutSnapshot resize(double mainWeight, List<ChartPane> panes, int dividerIndex, int deltaY, int totalHeight, int gapPx) {
        List<Integer> minHeights = new ArrayList<>();
        minHeights.add(getMainMinHeightPx());
        if (panes != null) {
            for (int i = 0; i < panes.size(); i++) {
                minHeights.add(MIN_PANE_HEIGHT_PX);
            }
        }
        return resize(mainWeight, panes, dividerIndex, deltaY, totalHeight, gapPx, minHeights);
    }

    static LayoutSnapshot resize(double mainWeight, List<ChartPane> panes, int dividerIndex, int deltaY, int totalHeight, int gapPx, List<Integer> minHeights) {
        int segmentCount = 1 + (panes == null ? 0 : panes.size());
        if (segmentCount < 2 || dividerIndex < 0 || dividerIndex >= segmentCount - 1) {
            return new LayoutSnapshot(mainWeight, copyPaneWeights(panes), computeHeights(mainWeight, panes, totalHeight, gapPx, minHeights));
        }

        List<Integer> heights = computeHeights(mainWeight, panes, totalHeight, gapPx, minHeights);
        int upperIndex = dividerIndex;
        int lowerIndex = dividerIndex + 1;

        int upper = heights.get(upperIndex);
        int lower = heights.get(lowerIndex);
        int upperMin = resolveMinHeight(minHeights, upperIndex);
        int lowerMin = resolveMinHeight(minHeights, lowerIndex);

        int newUpper = upper + deltaY;
        int newLower = lower - deltaY;

        if (newUpper < upperMin) {
            int deficit = upperMin - newUpper;
            newUpper = upperMin;
            newLower -= deficit;
        }
        if (newLower < lowerMin) {
            int deficit = lowerMin - newLower;
            newLower = lowerMin;
            newUpper -= deficit;
        }

        if (newUpper < upperMin) {
            newUpper = upperMin;
        }
        if (newLower < lowerMin) {
            newLower = lowerMin;
        }

        heights.set(upperIndex, newUpper);
        heights.set(lowerIndex, newLower);
        normalizeHeights(heights, Math.max(0, totalHeight), minHeights);

        double safeTotal = Math.max(1.0, totalHeight);
        double newMainWeight = Math.max(0.0001, heights.get(0) / safeTotal);
        List<Double> paneWeights = new ArrayList<>();
        for (int i = 1; i < heights.size(); i++) {
            paneWeights.add(Math.max(0.0001, heights.get(i) / safeTotal));
        }
        normalizeWeights(newMainWeight, paneWeights);
        newMainWeight = Math.max(0.0001, newMainWeightFrom(paneWeights));

        return new LayoutSnapshot(newMainWeight, paneWeights, heights);
    }

    private static double newMainWeightFrom(List<Double> paneWeights) {
        double paneSum = 0.0;
        for (Double weight : paneWeights) {
            paneSum += weight;
        }
        return Math.max(0.0001, 1.0 - paneSum);
    }

    private static List<Double> copyPaneWeights(List<ChartPane> panes) {
        List<Double> paneWeights = new ArrayList<>();
        if (panes != null) {
            for (ChartPane pane : panes) {
                paneWeights.add(Math.max(0.0001, pane.getHeightRatio()));
            }
        }
        return paneWeights;
    }

    private static void normalizeWeights(List<Double> weights) {
        double total = 0.0;
        for (Double weight : weights) {
            total += Math.max(0.0001, weight);
        }
        if (total <= 0.0) {
            double equal = 1.0 / Math.max(1, weights.size());
            for (int i = 0; i < weights.size(); i++) {
                weights.set(i, equal);
            }
            return;
        }
        for (int i = 0; i < weights.size(); i++) {
            weights.set(i, Math.max(0.0001, weights.get(i)) / total);
        }
    }

    private static void normalizeWeights(double mainWeight, List<Double> paneWeights) {
        List<Double> combined = new ArrayList<>();
        combined.add(Math.max(0.0001, mainWeight));
        combined.addAll(paneWeights);
        normalizeWeights(combined);
        paneWeights.clear();
        for (int i = 1; i < combined.size(); i++) {
            paneWeights.add(combined.get(i));
        }
    }

    private static void normalizeHeights(List<Integer> heights, int totalHeight, List<Integer> minHeights) {
        int sum = 0;
        for (Integer height : heights) {
            sum += height;
        }
        if (heights.isEmpty() || sum == totalHeight) {
            return;
        }

        int diff = totalHeight - sum;
        for (int i = heights.size() - 1; i >= 0 && diff != 0; i--) {
            int min = resolveMinHeight(minHeights, i);
            int current = heights.get(i);
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
    }


    private static int resolveMinHeight(List<Integer> minHeights, int index) {
        if (minHeights != null && index >= 0 && index < minHeights.size()) {
            return Math.max(1, minHeights.get(index));
        }
        return index == 0 ? getMainMinHeightPx() : MIN_PANE_HEIGHT_PX;
    }

    private static int getMainMinHeightPx() {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        return Math.max(1, Math.round(MAIN_MIN_HEIGHT_DP * (dpi / 160f)));
    }

    static final class LayoutSnapshot {
        private final double mainWeight;
        private final List<Double> paneWeights;
        private final List<Integer> heights;

        LayoutSnapshot(double mainWeight, List<Double> paneWeights, List<Integer> heights) {
            this.mainWeight = mainWeight;
            this.paneWeights = paneWeights;
            this.heights = heights;
        }

        double mainWeight() {
            return mainWeight;
        }

        List<Double> paneWeights() {
            return paneWeights;
        }

        List<Integer> heights() {
            return heights;
        }
    }
}

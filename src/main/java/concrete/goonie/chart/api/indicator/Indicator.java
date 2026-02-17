package concrete.goonie.chart.api.indicator;

import concrete.goonie.chart.api.data.DataSet;
import concrete.goonie.chart.api.history.HistoricalData;
import concrete.goonie.chart.api.model.Timeframe;

import java.util.List;

/**
 * Public indicator contract.
 */
public interface Indicator {
    String getName();
    Timeframe getCalculationTimeframe();
    boolean supports(Timeframe timeframe);
    void calculate(HistoricalData historicalData, String symbol);
    List<DataSet> getRenderableDataSets(Timeframe chartTimeframe);
}

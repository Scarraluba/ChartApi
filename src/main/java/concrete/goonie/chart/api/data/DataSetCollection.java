package concrete.goonie.chart.api.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable collection of datasets.
 */
public final class DataSetCollection {
    private final List<DataSet> dataSets = new ArrayList<>();

    public void add(DataSet dataSet) {
        if (dataSet != null && !dataSets.contains(dataSet)) {
            dataSets.add(dataSet);
        }
    }

    public void remove(DataSet dataSet) {
        dataSets.remove(dataSet);
    }

    public void clear() {
        dataSets.clear();
    }

    public List<DataSet> all() {
        return Collections.unmodifiableList(dataSets);
    }
}

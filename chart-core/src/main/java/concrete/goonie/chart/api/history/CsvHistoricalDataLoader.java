package concrete.goonie.chart.api.history;

import concrete.goonie.chart.api.model.Candle;
import concrete.goonie.chart.api.model.CandleSeries;
import concrete.goonie.chart.api.model.Timeframe;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal CSV loader for historical candles.
 */
public final class CsvHistoricalDataLoader {
    private CsvHistoricalDataLoader() {}

    public static CandleSeries load(Path path, boolean header, DateTimeFormatter formatter) throws IOException {
        CandleSeries series = new CandleSeries();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (first && header) {
                    first = false;
                    continue;
                }
                first = false;
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                LocalDateTime time = LocalDateTime.parse(parts[0].trim(), formatter);
                double open = Double.parseDouble(parts[1].trim());
                double high = Double.parseDouble(parts[2].trim());
                double low = Double.parseDouble(parts[3].trim());
                double close = Double.parseDouble(parts[4].trim());
                long volume = parts.length > 5 ? Long.parseLong(parts[5].trim()) : 0L;
                long spread = parts.length > 6 ? Long.parseLong(parts[6].trim()) : 0L;
                series.add(new Candle(time, open, high, low, close, volume, spread));
            }
        }
        return series;
    }

    public static void loadInto(HistoricalData historicalData, String symbol, Timeframe timeframe, Path path, boolean header, DateTimeFormatter formatter) throws IOException {
        historicalData.setSeries(symbol, timeframe, load(path, header, formatter));
    }
}

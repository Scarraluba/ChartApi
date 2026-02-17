package concrete.goonie.chart.api.model;

import java.time.Duration;

/**
 * Supported chart timeframes.
 */
public enum Timeframe {
    M1(Duration.ofMinutes(1)),
    M5(Duration.ofMinutes(5)),
    M15(Duration.ofMinutes(15)),
    M30(Duration.ofMinutes(30)),
    H1(Duration.ofHours(1)),
    H4(Duration.ofHours(4)),
    D1(Duration.ofDays(1));

    private final Duration duration;

    Timeframe(Duration duration) {
        this.duration = duration;
    }

    /** Returns the duration of this timeframe. */
    public Duration duration() {
        return duration;
    }

    /** Returns the timeframe size in seconds. */
    public long seconds() {
        return duration.getSeconds();
    }

    /** Returns true if this timeframe is the same as or higher than the other timeframe. */
    public boolean isSameOrHigherThan(Timeframe other) {
        return this.seconds() >= other.seconds();
    }
}

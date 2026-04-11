package concrete.goonie.chart.api.render;

/**
 * High-level axis range mode inspired by established XY chart libraries.
 */
public enum RangeType {
    /** Fully automatic range based on visible data. */
    AUTO,
    /** Uses fixed lower and/or upper bounds. */
    FIXED,
    /** Keeps the range at or above zero. */
    NON_NEGATIVE,
    /** Keeps the range strictly positive using a tiny positive floor. */
    POSITIVE,
    /** Clamps the visible range inside allowed min/max bounds. */
    CLAMPED,
    /** Tries to include zero while still adapting to the data. */
    ZERO_BASED
}

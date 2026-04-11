package concrete.goonie.chart.api.render;

/**
 * Axis range policy inspired by range controls commonly found in XY chart libraries.
 * Supports auto-range constraints such as non-negative ranges, fixed lower/upper bounds,
 * allowed min/max clamps, and higher-level range modes.
 */
public final class AxisRangePolicy {
    private boolean autoRange = true;
    private boolean nonNegative;
    private Double fixedLowerBound;
    private Double fixedUpperBound;
    private Double minAllowed;
    private Double maxAllowed;
    private RangeType rangeType = RangeType.AUTO;

    public boolean isAutoRange() {
        return autoRange;
    }

    public AxisRangePolicy setAutoRange(boolean autoRange) {
        this.autoRange = autoRange;
        return this;
    }

    public boolean isNonNegative() {
        return nonNegative;
    }

    public AxisRangePolicy setNonNegative(boolean nonNegative) {
        this.nonNegative = nonNegative;
        if (nonNegative && rangeType == RangeType.AUTO) {
            this.rangeType = RangeType.NON_NEGATIVE;
        }
        return this;
    }

    public Double getFixedLowerBound() {
        return fixedLowerBound;
    }

    public AxisRangePolicy setFixedLowerBound(Double fixedLowerBound) {
        this.fixedLowerBound = fixedLowerBound;
        if (fixedLowerBound != null && rangeType == RangeType.AUTO) {
            this.rangeType = RangeType.FIXED;
        }
        return this;
    }

    public Double getFixedUpperBound() {
        return fixedUpperBound;
    }

    public AxisRangePolicy setFixedUpperBound(Double fixedUpperBound) {
        this.fixedUpperBound = fixedUpperBound;
        if (fixedUpperBound != null && rangeType == RangeType.AUTO) {
            this.rangeType = RangeType.FIXED;
        }
        return this;
    }

    public Double getMinAllowed() {
        return minAllowed;
    }

    public AxisRangePolicy setMinAllowed(Double minAllowed) {
        this.minAllowed = minAllowed;
        if (minAllowed != null && rangeType == RangeType.AUTO) {
            this.rangeType = RangeType.CLAMPED;
        }
        return this;
    }

    public Double getMaxAllowed() {
        return maxAllowed;
    }

    public AxisRangePolicy setMaxAllowed(Double maxAllowed) {
        this.maxAllowed = maxAllowed;
        if (maxAllowed != null && rangeType == RangeType.AUTO) {
            this.rangeType = RangeType.CLAMPED;
        }
        return this;
    }

    public RangeType getRangeType() {
        return rangeType;
    }

    public AxisRangePolicy setRangeType(RangeType rangeType) {
        this.rangeType = rangeType == null ? RangeType.AUTO : rangeType;
        return this;
    }

    public AxisRangePolicy asAuto() {
        rangeType = RangeType.AUTO;
        autoRange = true;
        nonNegative = false;
        fixedLowerBound = null;
        fixedUpperBound = null;
        minAllowed = null;
        maxAllowed = null;
        return this;
    }

    public AxisRangePolicy asNonNegative() {
        rangeType = RangeType.NON_NEGATIVE;
        autoRange = true;
        nonNegative = true;
        fixedLowerBound = null;
        fixedUpperBound = null;
        return this;
    }

    public AxisRangePolicy asPositive() {
        rangeType = RangeType.POSITIVE;
        autoRange = true;
        nonNegative = true;
        fixedLowerBound = null;
        fixedUpperBound = null;
        return this;
    }

    public AxisRangePolicy asZeroBased() {
        rangeType = RangeType.ZERO_BASED;
        autoRange = true;
        fixedLowerBound = null;
        fixedUpperBound = null;
        return this;
    }

    public AxisRangePolicy asFixed(double lower, double upper) {
        rangeType = RangeType.FIXED;
        autoRange = false;
        fixedLowerBound = lower;
        fixedUpperBound = upper;
        return this;
    }

    public AxisRangePolicy asClamped(Double minAllowed, Double maxAllowed) {
        rangeType = RangeType.CLAMPED;
        autoRange = true;
        this.minAllowed = minAllowed;
        this.maxAllowed = maxAllowed;
        return this;
    }

    public AxisRangePolicy copy() {
        return new AxisRangePolicy()
                .setAutoRange(autoRange)
                .setNonNegative(nonNegative)
                .setFixedLowerBound(fixedLowerBound)
                .setFixedUpperBound(fixedUpperBound)
                .setMinAllowed(minAllowed)
                .setMaxAllowed(maxAllowed)
                .setRangeType(rangeType);
    }

    /**
     * Applies the policy to a proposed range and returns a normalized pair.
     */
    public double[] apply(double lower, double upper) {
        double lo = lower;
        double hi = upper;

        if (!Double.isFinite(lo) || !Double.isFinite(hi)) {
            lo = 0.0;
            hi = 1.0;
        }

        switch (rangeType) {
            case NON_NEGATIVE -> {
                lo = Math.max(0.0, lo);
                hi = Math.max(0.0, hi);
            }
            case POSITIVE -> {
                lo = Math.max(1e-9, lo);
                hi = Math.max(1e-9, hi);
            }
            case ZERO_BASED -> {
                lo = Math.min(0.0, lo);
                hi = Math.max(0.0, hi);
            }
            case FIXED, CLAMPED, AUTO -> {
                // explicit properties below handle these modes
            }
        }

        if (nonNegative) {
            lo = Math.max(0.0, lo);
            hi = Math.max(0.0, hi);
        }

        if (minAllowed != null) {
            lo = Math.max(minAllowed, lo);
            hi = Math.max(minAllowed, hi);
        }

        if (maxAllowed != null) {
            lo = Math.min(maxAllowed, lo);
            hi = Math.min(maxAllowed, hi);
        }

        if (fixedLowerBound != null) {
            lo = fixedLowerBound;
            if (hi < lo) {
                hi = lo;
            }
        }

        if (fixedUpperBound != null) {
            hi = fixedUpperBound;
            if (lo > hi) {
                lo = hi;
            }
        }

        if (Math.abs(hi - lo) <= 1e-9) {
            double anchor = lo;
            if (fixedLowerBound != null && fixedUpperBound != null) {
                hi = fixedUpperBound;
                lo = fixedLowerBound;
            } else if (fixedLowerBound != null) {
                hi = fixedLowerBound + 1.0;
            } else if (fixedUpperBound != null) {
                lo = fixedUpperBound - 1.0;
            } else {
                lo = anchor - 1.0;
                hi = anchor + 1.0;
                if ((nonNegative || rangeType == RangeType.NON_NEGATIVE || rangeType == RangeType.POSITIVE) && lo < 0.0) {
                    hi -= lo;
                    lo = rangeType == RangeType.POSITIVE ? 1e-9 : 0.0;
                }
            }
        }

        return new double[]{lo, hi};
    }
}

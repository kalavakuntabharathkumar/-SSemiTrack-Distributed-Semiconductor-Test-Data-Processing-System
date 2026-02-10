package com.semitrack.validator;

import com.semitrack.model.WaferTestResult;

/**
 * Validates that a specific parametric measurement falls within an acceptable
 * [min, max] range.
 *
 * Results that are missing the key entirely are treated as corrupted data and
 * rejected.  Results whose value is outside [min, max] are also rejected even
 * if the status is already marked PASS, preventing silently bad data from
 * reaching the aggregator.
 */
public class RangeValidator implements Validator {

    private final String parameterKey;
    private final double min;
    private final double max;

    public RangeValidator(String parameterKey, double min, double max) {
        if (min > max) throw new IllegalArgumentException("min must be <= max");
        this.parameterKey = parameterKey;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(WaferTestResult result) {
        if (result == null) return false;
        Double value = result.getParametricValues().get(parameterKey);
        if (value == null) return false;          // missing key → corrupted
        return value >= min && value <= max;
    }

    @Override
    public String description() {
        return String.format("RangeValidator [%s in %.4f..%.4f]", parameterKey, min, max);
    }
}

package com.semitrack.validator;

import com.semitrack.model.WaferTestResult;

/**
 * Contract for validating a {@link WaferTestResult} before it is accepted by
 * the aggregation store.
 *
 * Implementations may check coordinate bounds, parametric ranges, data
 * completeness, or any domain-specific rule.
 */
public interface Validator {

    /**
     * Validate the given test result.
     *
     * @param result the result to validate; must not be {@code null}
     * @return {@code true} if the result passes validation; {@code false} otherwise
     */
    boolean validate(WaferTestResult result);

    /**
     * Human-readable description of what this validator checks.
     */
    String description();
}

package com.semitrack.validator;

import com.semitrack.model.WaferTestResult;

/**
 * Ensures that die coordinates fall within the legal wafer map boundaries.
 *
 * A standard 300 mm wafer is modelled here as a grid from (0,0) to (maxX, maxY).
 * Results whose die position lies outside this grid are rejected as corrupted.
 */
public class CoordinateValidator implements Validator {

    private final int maxX;
    private final int maxY;

    public CoordinateValidator(int maxX, int maxY) {
        if (maxX < 0 || maxY < 0) {
            throw new IllegalArgumentException("Max coordinates must be non-negative");
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public boolean validate(WaferTestResult result) {
        if (result == null || result.getDie() == null) return false;
        int x = result.getDie().getX();
        int y = result.getDie().getY();
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }

    @Override
    public String description() {
        return String.format("CoordinateValidator [0..%d, 0..%d]", maxX, maxY);
    }
}

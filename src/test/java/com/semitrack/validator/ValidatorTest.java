package com.semitrack.validator;

import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferDie;
import com.semitrack.model.WaferTestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoordinateValidator} and {@link RangeValidator}.
 *
 * Covers normal paths, edge cases (boundary values), and corrupted-data
 * scenarios (null results, missing parametric keys).
 */
class ValidatorTest {

    // ── CoordinateValidator ───────────────────────────────────────────────────

    @Test
    @DisplayName("CoordinateValidator passes die at origin (0,0)")
    void coordinateValidator_passesOrigin() {
        CoordinateValidator v = new CoordinateValidator(24, 24);
        WaferTestResult r = makeResult(0, 0, Map.of("voltage", 1.2, "threshold", 0.5));
        assertTrue(v.validate(r));
    }

    @Test
    @DisplayName("CoordinateValidator passes die at max boundary")
    void coordinateValidator_passesMaxBoundary() {
        CoordinateValidator v = new CoordinateValidator(24, 24);
        WaferTestResult r = makeResult(24, 24, Map.of("voltage", 1.2, "threshold", 0.5));
        assertTrue(v.validate(r));
    }

    @Test
    @DisplayName("CoordinateValidator rejects die outside grid")
    void coordinateValidator_rejectsOutOfBounds() {
        CoordinateValidator v = new CoordinateValidator(24, 24);
        WaferTestResult r = makeResult(25, 0, Map.of("voltage", 1.2, "threshold", 0.5));
        assertFalse(v.validate(r));
    }

    @Test
    @DisplayName("CoordinateValidator rejects null result")
    void coordinateValidator_rejectsNull() {
        CoordinateValidator v = new CoordinateValidator(24, 24);
        assertFalse(v.validate(null));
    }

    @Test
    @DisplayName("CoordinateValidator constructor rejects negative max")
    void coordinateValidator_constructorRejectsNegativeMax() {
        assertThrows(IllegalArgumentException.class, () -> new CoordinateValidator(-1, 24));
    }

    // ── RangeValidator ────────────────────────────────────────────────────────

    @Test
    @DisplayName("RangeValidator passes value within range")
    void rangeValidator_passesInRange() {
        RangeValidator v = new RangeValidator("voltage", 0.0, 5.0);
        WaferTestResult r = makeResult(1, 1, Map.of("voltage", 2.5, "threshold", 0.5));
        assertTrue(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator passes value at lower boundary")
    void rangeValidator_passesAtMinBoundary() {
        RangeValidator v = new RangeValidator("voltage", 1.0, 5.0);
        WaferTestResult r = makeResult(1, 1, Map.of("voltage", 1.0, "threshold", 0.5));
        assertTrue(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator passes value at upper boundary")
    void rangeValidator_passesAtMaxBoundary() {
        RangeValidator v = new RangeValidator("voltage", 1.0, 5.0);
        WaferTestResult r = makeResult(1, 1, Map.of("voltage", 5.0, "threshold", 0.5));
        assertTrue(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator rejects value below range")
    void rangeValidator_rejectsBelowRange() {
        RangeValidator v = new RangeValidator("voltage", 1.0, 5.0);
        WaferTestResult r = makeResult(1, 1, Map.of("voltage", 0.5, "threshold", 0.5));
        assertFalse(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator rejects value above range")
    void rangeValidator_rejectsAboveRange() {
        RangeValidator v = new RangeValidator("voltage", 1.0, 5.0);
        WaferTestResult r = makeResult(1, 1, Map.of("voltage", 5.1, "threshold", 0.5));
        assertFalse(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator rejects result with missing key (corrupted data)")
    void rangeValidator_rejectsMissingKey() {
        RangeValidator v = new RangeValidator("voltage", 0.0, 5.0);
        // "voltage" key is absent – simulates corrupted / partial data
        WaferTestResult r = makeResult(1, 1, Map.of("threshold", 0.5));
        assertFalse(v.validate(r));
    }

    @Test
    @DisplayName("RangeValidator rejects null result")
    void rangeValidator_rejectsNull() {
        RangeValidator v = new RangeValidator("voltage", 0.0, 5.0);
        assertFalse(v.validate(null));
    }

    @Test
    @DisplayName("RangeValidator constructor rejects inverted range")
    void rangeValidator_constructorRejectsInvertedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new RangeValidator("voltage", 5.0, 1.0));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WaferTestResult makeResult(int x, int y, Map<String, Double> params) {
        return new WaferTestResult(
                "ST-TEST",
                "W-TEST",
                new WaferDie(x, y),
                TestStatus.PASS,
                new HashMap<>(params));
    }
}

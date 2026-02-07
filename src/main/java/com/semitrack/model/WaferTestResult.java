package com.semitrack.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures the full test outcome for one die on a wafer.
 *
 * Parametric values (e.g. voltage, threshold) are stored in an unmodifiable
 * {@link HashMap} so callers can read measurements without mutating the record.
 */
public class WaferTestResult {

    private final String stationId;
    private final String waferId;
    private final WaferDie die;
    private final TestStatus status;
    private final Map<String, Double> parametricValues;
    private final Instant timestamp;

    public WaferTestResult(String stationId,
                           String waferId,
                           WaferDie die,
                           TestStatus status,
                           Map<String, Double> parametricValues) {
        this.stationId        = stationId;
        this.waferId          = waferId;
        this.die              = die;
        this.status           = status;
        this.parametricValues = Collections.unmodifiableMap(new HashMap<>(parametricValues));
        this.timestamp        = Instant.now();
    }

    public String getStationId()          { return stationId; }
    public String getWaferId()            { return waferId; }
    public WaferDie getDie()              { return die; }
    public TestStatus getStatus()         { return status; }
    public Map<String, Double> getParametricValues() { return parametricValues; }
    public Instant getTimestamp()         { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] Wafer=%s Die=%s Status=%s Params=%s",
                stationId, waferId, die, status, parametricValues);
    }
}

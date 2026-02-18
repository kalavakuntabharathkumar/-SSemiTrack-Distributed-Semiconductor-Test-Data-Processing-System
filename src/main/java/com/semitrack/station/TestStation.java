package com.semitrack.station;

import com.semitrack.aggregator.AggregatorService;
import com.semitrack.config.AppConfig;
import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.WaferTestResult;
import com.semitrack.validator.Validator;

import java.util.List;

/**
 * Abstract base class for all test station types.
 *
 * <p>Enforces the OOP contract:
 * <ul>
 *   <li><b>Encapsulation</b> – station ID and aggregator are private/protected.</li>
 *   <li><b>Inheritance</b> – concrete stations extend this class and implement
 *       {@link #generateResults(String)} for their domain logic.</li>
 *   <li><b>Polymorphism</b> – callers only see {@code TestStation} and invoke
 *       {@link #runTests()}, which calls the overridden method at runtime.</li>
 * </ul>
 *
 * <p>The {@link #runTests()} method is {@code final} to ensure the submission
 * flow (generate → submit → log) is consistent across all station types.
 */
public abstract class TestStation {

    protected static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    private final String stationId;
    private final AggregatorService aggregator;
    protected final AppConfig config;

    protected TestStation(String stationId, AggregatorService aggregator) {
        this.stationId  = stationId;
        this.aggregator = aggregator;
        this.config     = AppConfig.getInstance();
    }

    // ── Template method ────────────────────────────────────────────────────

    /**
     * Runs the full test cycle for this station.
     *
     * For each wafer (count controlled by {@link AppConfig#getWafersPerStation()}):
     * <ol>
     *   <li>Delegate to {@link #generateResults(String)} for domain-specific data.</li>
     *   <li>Submit each result to the aggregator's blocking queue.</li>
     * </ol>
     *
     * This method is deliberately {@code final} – the generation logic varies by
     * station type but the submission protocol must remain consistent.
     */
    public final void runTests() {
        LOG.info(String.format("Station %s starting (%d wafers × %d dies).",
                stationId, config.getWafersPerStation(), config.getDiesPerWafer()));

        for (int w = 1; w <= config.getWafersPerStation(); w++) {
            String waferId = stationId + "-W" + String.format("%03d", w);
            List<WaferTestResult> results = generateResults(waferId);

            for (WaferTestResult result : results) {
                try {
                    aggregator.submit(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("Station " + stationId + " interrupted while submitting.");
                    return;
                }
            }
            LOG.info(String.format("Station %s submitted %d results for %s.",
                    stationId, results.size(), waferId));
        }
        LOG.info("Station " + stationId + " finished.");
    }

    /**
     * Generate a list of test results for the given wafer.
     *
     * Subclasses implement this to simulate their specific measurement approach
     * (parametric measurement, pass/fail binary test, etc.).
     *
     * @param waferId the wafer being tested
     * @return non-null list of results (may be empty if no dies were tested)
     */
    protected abstract List<WaferTestResult> generateResults(String waferId);

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getStationId() { return stationId; }
}

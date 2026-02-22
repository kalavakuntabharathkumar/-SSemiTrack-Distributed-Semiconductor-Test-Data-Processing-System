package com.semitrack.observer;

import com.semitrack.logger.SemiTrackLogger;
import com.semitrack.model.WaferTestResult;

import java.util.List;

/**
 * Concrete observer that logs a high-visibility alert to the SemiTrack logger
 * whenever a batch exceeds the failure threshold.
 *
 * In a real fab environment this would forward to a SECS/GEM event stream or
 * a PagerDuty-style alerting service.  The logging approach here keeps the
 * demo self-contained while still demonstrating the Observer pattern.
 */
public class AlertObserver implements BatchFailureObserver {

    private static final SemiTrackLogger LOG = SemiTrackLogger.getInstance();

    @Override
    public void onBatchFailure(List<WaferTestResult> batch, double failureRate) {
        LOG.warn(String.format(
                "BATCH FAILURE ALERT — %.1f%% failure rate in batch of %d results. " +
                "Immediate engineering review required.",
                failureRate * 100.0, batch.size()));

        // Log each failed die for traceability
        batch.stream()
             .filter(r -> com.semitrack.model.TestStatus.FAIL.equals(r.getStatus()))
             .forEach(r -> LOG.warn("  FAIL die " + r.getDie() +
                                    " on wafer " + r.getWaferId() +
                                    " from station " + r.getStationId()));
    }
}

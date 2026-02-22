package com.semitrack.observer;

import com.semitrack.model.WaferTestResult;

import java.util.List;

/**
 * Observer interface – notified when a batch of test results exceeds the
 * configured failure-rate threshold.
 *
 * Implementations may send alerts, write to an audit log, trigger re-tests,
 * or update a monitoring dashboard.  The aggregator calls all registered
 * observers synchronously after each batch is committed.
 */
public interface BatchFailureObserver {

    /**
     * Called when the failure ratio in {@code batch} exceeds the threshold.
     *
     * @param batch       the full batch that triggered the notification
     * @param failureRate actual failure ratio, e.g. 0.45 means 45 % FAIL
     */
    void onBatchFailure(List<WaferTestResult> batch, double failureRate);
}

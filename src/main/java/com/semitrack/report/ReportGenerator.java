package com.semitrack.report;

import com.semitrack.model.WaferTestResult;

import java.util.List;

/**
 * Strategy interface for rendering test result reports.
 *
 * Different implementations may write to stdout, a file, a REST endpoint, or a
 * dashboard.  Callers depend only on this interface, keeping the aggregator
 * decoupled from any specific output format.
 */
public interface ReportGenerator {

    /**
     * Generate a report from the supplied results.
     *
     * @param results snapshot of collected test results; may be empty but not null
     */
    void generate(List<WaferTestResult> results);

    /** Short name used in logs to identify which generator produced output. */
    String name();
}

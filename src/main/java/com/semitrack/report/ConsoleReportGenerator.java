package com.semitrack.report;

import com.semitrack.model.TestStatus;
import com.semitrack.model.WaferTestResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes a human-readable summary to standard output.
 *
 * Groups results by wafer ID and prints pass/fail counts together with the
 * die-level breakdown, making it easy to spot yield problems at a glance.
 */
public class ConsoleReportGenerator implements ReportGenerator {

    @Override
    public void generate(List<WaferTestResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("[ConsoleReport] No results to report.");
            return;
        }

        Map<String, List<WaferTestResult>> byWafer =
                results.stream().collect(Collectors.groupingBy(WaferTestResult::getWaferId));

        System.out.println("=== SemiTrack Console Report ===");
        byWafer.forEach((waferId, waferResults) -> {
            long passCount = waferResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.PASS).count();
            long failCount = waferResults.size() - passCount;
            double yieldPct = 100.0 * passCount / waferResults.size();

            System.out.printf("  Wafer %-10s | Dies: %3d | PASS: %3d | FAIL: %3d | Yield: %.1f%%%n",
                    waferId, waferResults.size(), passCount, failCount, yieldPct);

            // Per-die detail (TreeMap order guaranteed by WaferDie.compareTo)
            waferResults.stream()
                    .sorted((a, b) -> a.getDie().compareTo(b.getDie()))
                    .forEach(r -> System.out.printf("    Die %-8s %s%n", r.getDie(), r.getStatus()));
        });
        System.out.println("=================================");
    }

    @Override
    public String name() { return "ConsoleReportGenerator"; }
}
